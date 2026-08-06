# Rider Inspection Copy

Rider plugin that adds a project-level **Inspect Code (copyable results)** action
and copies the displayed Code Issues with file paths and line coordinates without
requiring navigation to every issue.

The plugin contains a Rider frontend and a ReSharper/Rider backend. The source in
this repository corresponds to the 0.7.7 build submitted to JetBrains Marketplace.

## Build

Requirements:

- Windows with .NET SDK and Visual Studio/MSBuild;
- Rider 2026.1 installed locally;
- JDK compatible with the Gradle wrapper.

The Gradle build currently uses `C:/Program Files/JetBrains/Rider2026.1` as its
local Rider SDK. Adjust the `local(...)` path in `build.gradle.kts` if Rider is
installed elsewhere.

Build the plugin package with:

```powershell
.\gradlew.bat buildPlugin
```

The resulting ZIP and NuGet package are written to `output/`.

## License

MIT. See [LICENSE](LICENSE).
