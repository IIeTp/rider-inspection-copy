using JetBrains.Application.BuildScript.Application.Zones;

namespace ReSharperPlugin.InspectionCopyBackend.Tests;

[ZoneMarker]
public class ZoneMarker : IRequire<InspectionCopyBackendTestEnvironmentZone>;
