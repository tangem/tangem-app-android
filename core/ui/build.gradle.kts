import java.security.MessageDigest

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

/**
 * Verifies that generated Kotlin token files match the current ds-tokens submodule.
 * If this fails, run: cd core/ui/token-gen && npm run build
 */
abstract class VerifyDesignTokensTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokensDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val iconsDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val hashFile: RegularFileProperty

    @get:OutputFile
    abstract val stampFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val hashFileValue = hashFile.get().asFile
        require(hashFileValue.exists()) {
            "Design tokens hash file not found: ${hashFileValue.absolutePath}\n" +
                "Run the token generator: cd core/ui/token-gen && npm run build"
        }

        val tokensDirValue = tokensDir.get().asFile
        require(tokensDirValue.exists() && tokensDirValue.isDirectory) {
            "ds-tokens submodule not found: ${tokensDirValue.absolutePath}\n" +
                "Run: git submodule update --init --recursive"
        }

        val iconsDirValue = iconsDir.get().asFile
        require(iconsDirValue.exists() && iconsDirValue.isDirectory) {
            "ds-tokens icons folder not found: ${iconsDirValue.absolutePath}\n" +
                "Run: git submodule update --init --recursive"
        }

        val tokensInputHash = hashTreeHex(tokensDirValue, "json")
        val iconsHash = hashTreeHex(iconsDirValue, "svg")

        // Mirror build-tokens.mjs: sha256(tokensInputHash + 0x00 + iconsHash), all hex strings.
        val outer = MessageDigest.getInstance("SHA-256")
        outer.update(tokensInputHash.toByteArray())
        outer.update(0)
        outer.update(iconsHash.toByteArray())
        val actual = outer.digest()
            .joinToString("") { b: Byte -> b.toInt().and(0xFF).toString(16).padStart(2, '0') }
        val expected = hashFileValue.readText().trim()

        require(actual == expected) {
            "Design tokens are out of date!\n" +
                "  ds-tokens hash: $actual\n" +
                "  generated hash:  $expected\n" +
                "Run the token generator: cd core/ui/token-gen && npm run build"
        }

        stampFile.get().asFile.writeText(actual)
    }

    private fun hashTreeHex(root: java.io.File, extension: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val files = root.walkTopDown()
            .filter { it.isFile && it.extension == extension }
            .sortedBy { it.relativeTo(root).invariantSeparatorsPath }
            .toList()
        val nul = byteArrayOf(0)
        for (file in files) {
            digest.update(file.relativeTo(root).invariantSeparatorsPath.toByteArray())
            digest.update(nul)
            digest.update(file.readBytes())
            digest.update(nul)
        }
        return digest.digest()
            .joinToString("") { b: Byte -> b.toInt().and(0xFF).toString(16).padStart(2, '0') }
    }
}
android {
    namespace = "com.tangem.core.ui"

    packaging {
        resources {
            // To build and run composable preview
            merges += "paymentrequest.proto"
        }
    }
}

val verifyDesignTokens = tasks.register<VerifyDesignTokensTask>("verifyDesignTokens") {
    tokensDir.set(file("ds-tokens/tokens"))
    iconsDir.set(file("ds-tokens/icons"))
    hashFile.set(file("src/main/java/com/tangem/core/ui/res/generated/.tokens-hash"))
    stampFile.set(layout.buildDirectory.file("tokens-verified.stamp"))
}

tasks.named("preBuild") {
    dependsOn(verifyDesignTokens)
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    // endregion

    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    api(deps.kotlin.serialization.core)
    // endregion

    // region Compose
    implementation(deps.compose.accompanist.permission)
    api(deps.compose.accompanist.systemUiController)
    api(deps.compose.coil)
    implementation(deps.compose.constraintLayout)
    api(deps.compose.foundation)
    api(deps.compose.material3)
    api(deps.compose.paging)
    api(deps.compose.reorderable)
    api(deps.compose.shimmer)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    // endregion

    // region AndroidX
    api(deps.androidx.activity)
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.core)
    implementation(deps.androidx.core.ktx)
    implementation(deps.androidx.paging.runtime)
    api(deps.androidx.palette)
    implementation(deps.androidx.savedState)
    implementation(deps.androidx.windowManager) {
        exclude(
            deps.kotlin.coroutines.android.get().module.group,
            deps.kotlin.coroutines.android.get().module.name,
        )
    }
    api(deps.lifecycle.compose)
    api(deps.lifecycle.runtime.ktx)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(deps.haze) {
        exclude(module = "activity-compose")
        exclude(module = "activity")
        exclude(module = "activity-ktx")
    }
    api(deps.haze.materials) {
        exclude(module = "activity-compose")
        exclude(module = "activity")
        exclude(module = "activity-ktx")
    }
    api(deps.jodatime)
    api(deps.markdown)
    implementation(deps.material)
    implementation(deps.zxing.qrCore)
    // endregion

    // region Core modules
    api(projects.core.decompose)
    api(projects.core.error)
    implementation(projects.core.res)
    implementation(projects.core.utils)
    // endregion

    // region Domain models
    api(projects.domain.appTheme.models)
    api(projects.domain.express.models)
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}