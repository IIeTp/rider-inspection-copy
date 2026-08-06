package com.codex.rider.inspectioncopy;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.PopupHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JTree;
import java.awt.Component;
import java.awt.Container;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Adds a copy-only action to Rider's standard Code Issues result panel. */
public final class RiderInspectionResultsStartupActivity implements StartupActivity.DumbAware {
  private static final String PROBLEMS_TOOL_WINDOW_ID = "Problems View";
  private static final String RIDER_RESULT_PANEL =
    "com.jetbrains.rider.inspections.RiderInspectionsResultPanel";

  @Override
  public void runActivity(@NotNull Project project) {
    Installer installer = new Installer(project);
    Disposer.register(project, installer);
    installer.start();
  }

  private static final class Installer implements com.intellij.openapi.Disposable {
    private final Project project;
    private final Set<JComponent> installedPanels =
      Collections.newSetFromMap(new IdentityHashMap<>());
    private javax.swing.Timer timer;

    private Installer(Project project) {
      this.project = project;
    }

    private void start() {
      ApplicationManager.getApplication().invokeLater(() -> {
        if (project.isDisposed()) return;

        timer = new javax.swing.Timer(500, event -> scan());
        timer.setInitialDelay(250);
        timer.start();
        scan();
      });
    }

    private void scan() {
      if (project.isDisposed()) return;

      ToolWindow toolWindow = ToolWindowManager.getInstance(project)
        .getToolWindow(PROBLEMS_TOOL_WINDOW_ID);
      if (toolWindow == null) return;

      ContentManager contentManager = toolWindow.getContentManager();
      for (Content content : contentManager.getContents()) {
        JComponent panel = findRiderResultPanel(content.getComponent());
        if (panel != null && installedPanels.add(panel)) install(panel);
      }
    }

    private void install(JComponent panel) {
      if (!(panel instanceof SimpleToolWindowPanel)) return;

      JTree tree = findTree(panel);
      if (tree == null) return;

      CopyCurrentInspectionResultsAction copyAction =
        new CopyCurrentInspectionResultsAction(panel, tree);
      DefaultActionGroup actionGroup = new DefaultActionGroup(copyAction);
      PopupHandler.installPopupMenu(tree, actionGroup, "Codex.RiderInspectionCopy.Popup");
    }

    private static JComponent findRiderResultPanel(Component component) {
      if (component instanceof JComponent &&
          RIDER_RESULT_PANEL.equals(component.getClass().getName())) {
        return (JComponent) component;
      }
      if (component instanceof Container) {
        for (Component child : ((Container) component).getComponents()) {
          JComponent result = findRiderResultPanel(child);
          if (result != null) return result;
        }
      }
      return null;
    }

    private static JTree findTree(Component component) {
      if (component instanceof JTree) return (JTree) component;
      if (component instanceof Container) {
        for (Component child : ((Container) component).getComponents()) {
          JTree result = findTree(child);
          if (result != null) return result;
        }
      }
      return null;
    }

    @Override
    public void dispose() {
      if (timer != null) {
        timer.stop();
        timer = null;
      }
      installedPanels.clear();
    }
  }
}
