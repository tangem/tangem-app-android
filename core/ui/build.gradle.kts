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

        val digest = MessageDigest.getInstance("SHA-256")
        val jsonFiles = tokensDirValue.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .sortedBy { it.relativeTo(tokensDirValue).path }
            .toList()

        val nul = byteArrayOf(0)
        for (file in jsonFiles) {
            digest.update(file.relativeTo(tokensDirValue).invariantSeparatorsPath.toByteArray())
            digest.update(nul)
            digest.update(file.readBytes())
            digest.update(nul)
        }

        val actual = digest.digest()
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
    hashFile.set(file("src/main/java/com/tangem/core/ui/res/generated/.tokens-hash"))
    stampFile.set(layout.buildDirectory.file("tokens-verified.stamp"))
}

tasks.named("preBuild") {
    dependsOn(verifyDesignTokens)
}

dependencies {
    /** Project - Domain */
    implementation(projects.domain.appTheme.models)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)

    /** Project - Core */
    implementation(projects.core.res)
    implementation(projects.core.utils)
    api(projects.core.decompose)
    implementation(projects.core.error)

    /** AndroidX libraries */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.lifecycle.runtime.ktx)
    implementation(deps.androidx.palette)
    implementation(deps.androidx.windowManager) {
        exclude(
            deps.kotlin.coroutines.android.get().module.group,
            deps.kotlin.coroutines.android.get().module.name
        )
    }

    /** Compose */
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.paging)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    implementation(deps.compose.coil)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    api(deps.compose.reorderable)

    /** Other libraries */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.accompanist.permission)
    implementation(deps.material)
    implementation(deps.compose.shimmer)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.zxing.qrCore)
    api(deps.jodatime)
    implementation(deps.markdown)
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

    /** Tests */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
}