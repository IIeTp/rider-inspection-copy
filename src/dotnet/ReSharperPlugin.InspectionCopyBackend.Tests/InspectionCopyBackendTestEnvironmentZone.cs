using System.Threading;
using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.ReSharper.TestFramework;
using JetBrains.TestFramework.Application.Zones;
using NUnit.Framework;

[assembly: Apartment(ApartmentState.STA)]

namespace ReSharperPlugin.InspectionCopyBackend.Tests;

/// <summary>
/// Configures the ReSharper test environment for backend protocol tests.
/// </summary>
[ZoneDefinition]
public class InspectionCopyBackendTestEnvironmentZone : ITestsEnvZone, IRequire<PsiFeatureTestZone>, IRequire<IInspectionCopyBackendZone>
{
}
