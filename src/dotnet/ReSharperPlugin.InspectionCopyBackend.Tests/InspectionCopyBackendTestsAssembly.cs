using JetBrains.TestFramework;
using NUnit.Framework;

namespace ReSharperPlugin.InspectionCopyBackend.Tests;

[SetUpFixture]
/// <summary>
/// Registers the test environment assembly for backend tests.
/// </summary>
public class InspectionCopyBackendTestsAssembly : ExtensionTestEnvironmentAssembly<InspectionCopyBackendTestEnvironmentZone>;
