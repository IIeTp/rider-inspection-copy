using JetBrains.TestFramework;
using NUnit.Framework;

namespace ReSharperPlugin.InspectionCopyBackend.Tests;

/// <summary>
/// Registers the test environment assembly for backend tests.
/// </summary>
[SetUpFixture]
public class InspectionCopyBackendTestsAssembly : ExtensionTestEnvironmentAssembly<InspectionCopyBackendTestEnvironmentZone>
{
}
