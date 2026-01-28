import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.util.concurrent.ConcurrentHashMap

plugins {
    alias(deps.plugins.kotlin.android) apply false
    alias(deps.plugins.kotlin.jvm) apply false
    alias(deps.plugins.kotlin.serialization) apply false
    alias(deps.plugins.kotlin.kapt) apply false
    alias(deps.plugins.android.application) apply false
    alias(deps.plugins.android.library) apply false
    alias(deps.plugins.hilt.android) apply false
    alias(deps.plugins.google.services) apply false
    alias(deps.plugins.firebase.crashlytics) apply false
    alias(deps.plugins.firebase.perf) apply false
    alias(deps.plugins.room) apply false
    alias(deps.plugins.kotlin.compose.compiler) apply false
    alias(deps.plugins.ksp) apply false
}

buildscript {
    dependencies {
        classpath(deps.gradle.android)
        classpath(deps.agconnect.agcp)
    }
}

val clean by tasks.registering {
    delete(rootProject.buildDir)
}

interface Injected {
    @get:Inject
    val fs: FileSystemOperations
}

data class TestStats(
    val total: Long = 0,
    val passed: Long = 0,
    val failed: Long = 0,
    val skipped: Long = 0,
)

val testResultsByModule = ConcurrentHashMap<String, TestStats>()

// Test task to run unit tests for debug/googleDebug variant (Android) and all JVM modules
val unitTest by tasks.registering {
    group = "verification"
    description = "Run unit tests for debug/googleDebug variant and all JVM modules"

    doLast {
        if (testResultsByModule.isNotEmpty()) {
            val totalStats = testResultsByModule.values.fold(TestStats()) { acc, stats ->
                TestStats(
                    total = acc.total + stats.total,
                    passed = acc.passed + stats.passed,
                    failed = acc.failed + stats.failed,
                    skipped = acc.skipped + stats.skipped,
                )
            }

            println("\n" + "=".repeat(80))
            println("TEST SUMMARY")
            println("=".repeat(80))

            testResultsByModule.toSortedMap().forEach { (module, stats) ->
                println("  $module: ${stats.total} tests (${stats.passed} passed, ${stats.failed} failed, ${stats.skipped} skipped)")
            }

            println("-".repeat(80))
            println("TOTAL: ${totalStats.total} tests in ${testResultsByModule.size} modules")
            println("  Passed:  ${totalStats.passed}")
            println("  Failed:  ${totalStats.failed}")
            println("  Skipped: ${totalStats.skipped}")
            println("=".repeat(80))
        }
    }
}

// Test Logging and testCI dependencies
subprojects {
    tasks.withType<Test>().configureEach {
        println("Test task scheduled: $path")

        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true

            afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
                if (desc.parent == null) { // will match the outermost suite
                    testResultsByModule[path] = TestStats(
                        total = result.testCount,
                        passed = result.successfulTestCount,
                        failed = result.failedTestCount,
                        skipped = result.skippedTestCount,
                    )

                    val output =
                        "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                    val startItem = "| "
                    val endItem = " |"
                    val repeatLength = startItem.length + output.length + endItem.length
                    println(
                        "\n" + "-".repeat(repeatLength) + "\n" + startItem + output + endItem + "\n" + "-".repeat(
                            repeatLength
                        )
                    )
                }
            }))
        }
    }

    // Register testCI dependencies
    // App module
    plugins.withId("com.android.application") {
        afterEvaluate {
            unitTest.configure { dependsOn(tasks.named("testGoogleDebugUnitTest")) }
        }
    }

    // Android libraries
    plugins.withId("com.android.library") {
        afterEvaluate {
            unitTest.configure { dependsOn(tasks.named("testDebugUnitTest")) }
        }
    }

    // Jvm modules
    plugins.withId("org.jetbrains.kotlin.jvm") {
        if (!plugins.hasPlugin("com.android.library") && !plugins.hasPlugin("com.android.application")) {
            unitTest.configure { dependsOn(tasks.named("test")) }
        }
    }
}

val assembleInternalQA by tasks.registering {
    group = "build"
    description = "Builds internal APK to 'build/outputs' directory"

    val appOutputApkDir = "$projectDir/app/build/outputs/apk/internal"
    val rootOutputApkDir = "$buildDir/outputs"
    val injected = objects.newInstance<Injected>()

    dependsOn(":app:assembleInternal")

    doFirst {
        injected.fs.delete {
            delete(appOutputApkDir)
            delete("$rootOutputApkDir/app-internal.apk")
        }
    }
    doLast {
        injected.fs.copy {
            from("$appOutputApkDir/app-internal.apk")
            into(rootOutputApkDir)
        }
    }
}

val assembleExternalQA by tasks.registering {
    group = "build"
    description = "Builds external APK to 'build/outputs' directory"

    val appOutputApkDir = "$projectDir/app/build/outputs/apk/external"
    val rootOutputApkDir = "$buildDir/outputs"
    val injected = objects.newInstance<Injected>()

    dependsOn(":app:assembleExternal")

    doFirst {
        injected.fs.delete {
            delete(appOutputApkDir)
            delete("$rootOutputApkDir/app-external.apk")
        }
    }
    doLast {
        injected.fs.copy {
            from("$appOutputApkDir/app-external.apk")
            into(rootOutputApkDir)
        }
    }
}

val assembleQA by tasks.registering {
    group = "build"
    description = "Builds internal and external APKs to 'build/outputs' directory"

    dependsOn(assembleInternalQA)
    dependsOn(assembleExternalQA)
}

val generateComposeMetrics by tasks.registering {
    group = "other"
    description = "Build external APK and generates compose metrics to 'build/compose-metrics' directory"

    subprojects {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            if (name.contains("compile")) {
                val outputDirectory = "${project.buildDir.absolutePath}/compose_metrics"
                compilerOptions {
                    freeCompilerArgs.addAll(
                        listOf(
                            "-P",
                            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$outputDirectory",
                            "-P",
                            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$outputDirectory"
                            //     "-P",
                            //     "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
                        )
                    )
                }
            }
        }
    }

    finalizedBy(assembleExternalQA)
}