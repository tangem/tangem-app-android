package com.tangem.plugin.configuration.configurations

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.io.Serializable

internal fun Project.configureTestLogging() {
    tasks.withType(Test::class.java).configureEach {
        println("Test task scheduled: $path")
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
        addTestListener(TestSuiteLogger(path))
    }
}

private class TestSuiteLogger(private val taskPath: String) : TestListener, Serializable {
    override fun beforeSuite(suite: TestDescriptor) {}

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        if (suite.parent == null) {
            val output =
                "$taskPath - Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
            val startItem = "| "
            val endItem = " |"
            val repeatLength = startItem.length + output.length + endItem.length
            println(
                "\n" + "-".repeat(repeatLength) + "\n" + startItem + output + endItem + "\n" + "-".repeat(
                    repeatLength,
                ),
            )
        }
    }

    override fun beforeTest(testDescriptor: TestDescriptor) {}
    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}

    companion object {
        private const val serialVersionUID = 1L
    }
}