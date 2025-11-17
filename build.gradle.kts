import org.gradle.api.tasks.testing.logging.TestExceptionFormat

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

// Test Logging
subprojects {
    tasks.withType<Test>().configureEach {
        val taskName = name.lowercase()
        if (taskName.contains("external") ||
            taskName.contains("internal") ||
            taskName.contains("release") ||
            taskName.contains("mocked") ||
            taskName.contains("huawei")
        ) {
            enabled = false
            println("Skipping test task: $name")
        } else {
            println("Test task scheduled: $name")
        }

        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true

            afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
                if (desc.parent == null) { // will match the outermost suite
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