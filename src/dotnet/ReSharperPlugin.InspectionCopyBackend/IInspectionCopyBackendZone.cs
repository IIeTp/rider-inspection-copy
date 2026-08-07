using JetBrains.Application.BuildScript.Application.Zones;

namespace ReSharperPlugin.InspectionCopyBackend;

/// <summary>
/// Provides the backend zone for the inspection copy protocol component.
/// </summary>
[ZoneDefinition]
public interface IInspectionCopyBackendZone : IZone
{
}
