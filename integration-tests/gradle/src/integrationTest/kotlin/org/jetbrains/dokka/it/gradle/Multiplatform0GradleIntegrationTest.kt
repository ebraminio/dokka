package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.*

class Multiplatform0GradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("6.6", "6.1.1"),
            kotlinVersions = listOf("1.4.0")
        )
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-multiplatform-0")
        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }
        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @Test
    @Ignore("KLIB is currently not supported, planned for 1.6.21")
    fun execute() {
        val result = createGradleRunner("dokkaHtml", "-i", "-s").buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtml")).outcome)

        val dokkaOutputDir = File(projectDir, "build/dokka/html")
        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka output directory")

        dokkaOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
