using JetBrains.Application.BuildScript.Application.Zones;

namespace ReSharperPlugin.InspectionCopyBackend.Tests;

[ZoneMarker]
/// <summary>
/// Marks the test environment zone as required by the test assembly.
/// </summary>
public class ZoneMarker : IRequire<InspectionCopyBackendTestEnvironmentZone>;
