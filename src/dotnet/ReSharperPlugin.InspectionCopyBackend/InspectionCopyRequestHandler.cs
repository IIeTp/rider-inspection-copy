using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Globalization;
using System.Linq;
using System.Reflection;
using JetBrains.Application.Parts;
using JetBrains.DocumentModel;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Daemon.SolutionAnalysis.ErrorsView;
using JetBrains.ReSharper.Feature.Services.Occurrences;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Psi;
using JetBrains.Rider.Model;
using JetBrains.Rider.Model.Inspections;

namespace ReSharperPlugin.InspectionCopyBackend;

/// <summary>
/// Serves copy requests for the result session that Rider has already shown.
/// It deliberately does not invoke RunInspection and does not fire NavigateTo.
/// </summary>
[SolutionComponent(Instantiation.ContainerAsyncPrimaryThread)]
public sealed class InspectionCopyRequestHandler
{
  private static readonly char[] RequestSeparators = { ',' };
  private readonly ISolution solution;
  private readonly InspectionCopyModel model;

  /// <summary>
  /// Initializes the request handler and subscribes it to the protocol model.
  /// </summary>
  public InspectionCopyRequestHandler(ISolution solution)
  {
    this.solution = solution;
    model = solution.GetProtocolSolution().GetInspectionCopyModel();
    Log("handler constructed");
    model.Start.Advise(solution.GetSolutionLifetimes().UntilSolutionCloseLifetime, HandleRequest);
    Log("start signal subscribed");
  }

  [SuppressMessage("Design", "CA1031", Justification =
    "The protocol boundary must convert any request failure into a model error.")]
  private void HandleRequest(string request)
  {
    Log($"request received: {request?.Length ?? 0} characters");
    model.Error.Value = string.Empty;
    model.Result.Value = string.Empty;

    try
    {
      var separator = request.IndexOf('|');
      var responseSeparator = request.IndexOf('|', separator + 1);
      if (separator <= 0 || responseSeparator <= separator + 1)
        throw new InvalidOperationException("Invalid inspection result request.");

      var requestId = request.Substring(separator + 1, responseSeparator - separator - 1);

      if (!long.TryParse(request.Substring(0, separator), out var resultModelId))
        throw new InvalidOperationException("Invalid Rider inspection result model id.");

      var indices = request.Substring(responseSeparator + 1)
        .Split(RequestSeparators, StringSplitOptions.RemoveEmptyEntries)
        .Select(int.Parse)
        .Distinct()
        .ToArray();

      var resultModel = FindResultModel(resultModelId);
      if (resultModel == null)
        throw new InvalidOperationException("The selected Rider Code Issues session is no longer available.");

      var occurrences = FindIndexedOccurrences(resultModel);
      if (occurrences == null)
        throw new InvalidOperationException("Rider did not expose locations for the current Code Issues session.");

      var report = BuildReport(occurrences, indices);
      Log($"response built: resultModelId={resultModelId}, occurrences={occurrences.Count}, " +
          $"indices={indices.Length}, report={report.Length} characters");
      model.Result.Value = requestId + "|" + report;
    }
    catch (Exception exception)
    {
      var responseSeparator = request.IndexOf('|');
      var secondSeparator = responseSeparator < 0 ? -1 : request.IndexOf('|', responseSeparator + 1);
      var requestId = secondSeparator > responseSeparator
        ? request.Substring(responseSeparator + 1, secondSeparator - responseSeparator - 1)
        : "unknown";
      Log($"request failed: {exception.GetBaseException().GetType().Name}: " +
          exception.GetBaseException().Message);
      model.Error.Value = requestId + "|Inspection copy failed: " + exception.GetBaseException().Message;
    }
  }

  [SuppressMessage("Design", "CA1031", Justification =
    "Logging is best effort and must never break the inspection result path.")]
  private static void Log(string message)
  {
    try
    {
      Console.Error.WriteLine($"[InspectionCopyBackend] {DateTime.Now:O} {message}");
    }
    catch
    {
      // Diagnostics must never affect the inspection result path.
    }
  }

  private InspectionResultsModel FindResultModel(long resultModelId)
  {
    var inspections = solution.GetProtocolSolution().GetInspectionsModel();
    foreach (var entry in inspections.InspectionsResultSessions)
    {
      if (entry.Value.RdId.Value == resultModelId)
        return entry.Value;
    }

    return null;
  }

  private static List<IOccurrence> FindIndexedOccurrences(InspectionResultsModel resultModel)
  {
    var navigateField = FindField(resultModel.GetType(), "_NavigateTo");
    var navigateSignal = navigateField?.GetValue(resultModel);
    var signalField = FindField(navigateSignal?.GetType(), "mySignal");
    var signal = signalField?.GetValue(navigateSignal);
    var listenersField = FindField(signal?.GetType(), "myListeners");
    var listeners = listenersField?.GetValue(signal);
    var itemsField = FindField(listeners?.GetType(), "myItems");
    var items = itemsField?.GetValue(listeners) as IEnumerable;
    var sizeField = FindField(listeners?.GetType(), "mySize");
    var sizeValue = sizeField?.GetValue(listeners);
    var size = sizeValue == null ? int.MaxValue : Convert.ToInt32(sizeValue, CultureInfo.InvariantCulture);

    if (items == null) return null;

    var index = 0;
    foreach (var item in items)
    {
      if (index++ >= size) break;

      var value = GetProperty(item, "Value");
      if (value is Delegate handler)
      {
        var occurrences = FindOccurrenceList(handler.Target, new HashSet<object>());
        if (occurrences != null) return occurrences;
      }
    }

    return null;
  }

  [SuppressMessage("Design", "CA1031", Justification =
    "Reflection probes private Rider implementation details that vary by IDE version.")]
  private static List<IOccurrence> FindOccurrenceList(object value, ISet<object> visited)
  {
    if (value == null || !visited.Add(value)) return null;

    var valueType = value.GetType();
    if (value is IEnumerable enumerable && value is not string && IsOccurrenceList(valueType))
      return enumerable.Cast<object>().OfType<IOccurrence>().ToList();

    if (!valueType.Name.Contains("DisplayClass", StringComparison.Ordinal)) return null;

    foreach (var field in AllFields(valueType))
    {
      object fieldValue;
      try
      {
        fieldValue = field.GetValue(value);
      }
      catch (Exception)
      {
        continue;
      }

      var result = FindOccurrenceList(fieldValue, visited);
      if (result != null) return result;
    }

    return null;
  }

  private static bool IsOccurrenceList(Type type)
  {
    if (!typeof(IEnumerable).IsAssignableFrom(type)) return false;
    var genericArguments = type.GetGenericArguments();
    return genericArguments.Length == 1 &&
           typeof(IOccurrence).IsAssignableFrom(genericArguments[0]);
  }

  private static IEnumerable<FieldInfo> AllFields(Type type)
  {
    for (var current = type; current != null; current = current.BaseType)
    {
      foreach (var field in current.GetFields(BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public))
        yield return field;
    }
  }

  private static FieldInfo FindField(Type type, string name)
  {
    return type == null ? null : AllFields(type).FirstOrDefault(field => field.Name == name);
  }

  private static object GetProperty(object value, string name)
  {
    return value?.GetType().GetProperty(name, BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic)
      ?.GetValue(value);
  }

  private static string BuildReport(List<IOccurrence> occurrences, IReadOnlyCollection<int> indices)
  {
    var lines = new List<string>();

    foreach (var index in indices)
    {
      if (index < 0 || index >= occurrences.Count) continue;
      if (!(occurrences[index] is IIssueOccurrence occurrence)) continue;

      var issue = occurrence.Issue;
      var sourceFile = issue.File.File;
      if (sourceFile == null || !sourceFile.IsValid()) continue;

      var projectFile = sourceFile.ToProjectFile();
      var path = projectFile == null ? sourceFile.Name : projectFile.Location.FullPath;
      var startOffset = issue.Range.HasValue ? issue.Range.Value.StartOffset : issue.NavigationOffset;
      var endOffset = issue.Range.HasValue ? issue.Range.Value.EndOffset : startOffset;
      startOffset = ClampOffset(sourceFile, startOffset);
      endOffset = ClampOffset(sourceFile, endOffset);

      var start = new DocumentOffset(sourceFile.Document, startOffset).ToDocumentCoords();
      var end = new DocumentOffset(sourceFile.Document, endOffset).ToDocumentCoords();
      lines.Add($"{path}:{(int)start.Line + 1}:{(int)start.Column + 1}-" +
                $"{(int)end.Line + 1}:{(int)end.Column + 1} " +
                issue.Message);
    }

    return lines.Count == 0 ? "No selected inspection issues found." : string.Join(Environment.NewLine, lines);
  }

  private static int ClampOffset(IPsiSourceFile sourceFile, int offset)
  {
    return Math.Max(0, Math.Min(offset, sourceFile.Document.GetTextLength()));
  }
}
