using JetBrains.Application.BuildScript.Application.Zones;

namespace ReSharperPlugin.InspectionCopyBackend.Tests;

/// <summary>
/// Marks the test environment zone as required by the test assembly.
/// </summary>
[ZoneMarker]
public class ZoneMarker : IRequire<InspectionCopyBackendTestEnvironmentZone>
{
}
