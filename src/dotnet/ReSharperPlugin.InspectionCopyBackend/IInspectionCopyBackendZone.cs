using JetBrains.Application.BuildScript.Application.Zones;

namespace ReSharperPlugin.InspectionCopyBackend;

[ZoneDefinition]
// [ZoneDefinitionConfigurableFeature("Title", "Description", IsInProductSection: false)]
/// <summary>
/// Provides the backend zone for the inspection copy protocol component.
/// </summary>
public interface IInspectionCopyBackendZone : IZone;
