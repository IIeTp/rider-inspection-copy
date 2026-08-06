# Rider Inspection Copy

Rider plugin that adds a project-level **Inspect Code (copyable results)** action
and copies the displayed Code Issues with file paths and line coordinates without
requiring navigation to every issue.

The plugin contains a Rider frontend and a ReSharper/Rider backend. The source in
this repository corresponds to the 0.7.8 build submitted to JetBrains Marketplace.

## Build locally

Requirements:

- .NET SDK;
- JDK 21;
- Rider 2026.1, either installed locally or downloaded by Gradle.

To use an installed Rider and avoid downloading another IDE archive:

```powershell
.\gradlew.bat buildPlugin
```

Use the following command to build a Release package from an installed Rider:

```powershell
.\gradlew.bat buildPlugin -PBuildConfiguration=Release -PriderPath="C:/Program Files/JetBrains/Rider2026.1"
```

Without `-PriderPath`, Gradle resolves the Rider SDK from JetBrains repositories.
The resulting ZIP and NuGet package are written to `output/`.

## GitHub Actions

The workflow in `.github/workflows/release.yml` builds the plugin on GitHub for
every push and pull request. It downloads the Rider SDK itself and does not
require Rider or Visual Studio to be committed to the repository.

To publish a GitHub Release, create a tag such as `v0.7.8`, or run the workflow
manually with a version tag. The workflow uploads only the built plugin ZIP.

## License

MIT. See [LICENSE](LICENSE).
