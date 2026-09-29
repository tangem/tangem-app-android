plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.spend.pushProvisioning.impl"

    testOptions.unitTests.isReturnDefaultValues = true
}

abstract class CopyMeaWalletConfigTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val configFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        configFile.get().asFile.copyTo(outputDir.file("raw/mea_config").get().asFile, overwrite = true)
    }
}

val meaWalletConfig: RegularFile = rootProject.layout.projectDirectory
    .file("app/src/main/assets/tangem-app-config/meawallet/mea_config")

androidComponents {
    onVariants { variant ->
        if (variant.buildType == "mocked") return@onVariants

        val copyConfig = tasks.register<CopyMeaWalletConfigTask>(
            "copy${variant.name.replaceFirstChar(Char::uppercaseChar)}MeaWalletConfig",
        ) {
            configFile.set(meaWalletConfig)
        }
        checkNotNull(variant.sources.res) { "${variant.name} has no Android resources for mea_config" }
            .addGeneratedSourceDirectory(copyConfig, CopyMeaWalletConfigTask::outputDir)
    }
}

dependencies {
    api(projects.spend.pushProvisioning.api)

    implementation(projects.core.utils)

    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)

    implementation(deps.googlePlay.services.tapAndPay)
    debugImplementation(deps.meawallet.mpp.test.debug)
    mockedImplementation(deps.meawallet.mpp.test.debug)
    internalImplementation(deps.meawallet.mpp.test)
    externalImplementation(deps.meawallet.mpp.prod)
    releaseImplementation(deps.meawallet.mpp.prod)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    testImplementation(projects.test.core)
}