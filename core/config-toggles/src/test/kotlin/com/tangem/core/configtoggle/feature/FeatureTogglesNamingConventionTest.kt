package com.tangem.core.configtoggle.feature

import com.google.common.truth.Truth
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.storage.ConfigToggle
import org.junit.jupiter.api.Test
import java.io.File

internal class FeatureTogglesNamingConventionTest {

    @Test
    fun `all new feature toggles must follow AND_id or TWI_id naming`() {
        val toggles = CONFIG_FILES.flatMap(::parseToggles)

        val invalid = toggles
            .map(ConfigToggle::name)
            .filterNot { it in EXCLUDED_TOGGLES_LIST }
            .filterNot(VALID_NAME_PATTERN::matches)

        Truth.assertWithMessage(
            """New feature toggles must match pattern ${VALID_NAME_PATTERN.pattern} — AND_<ticket_id> (Android ticket, e.g. AND_15312_PUSH_NOTIFICATION_SETTINGS_ENABLED) or TWI_<ticket_id> (idea ticket, e.g. TWI_1403_PUSH_NOTIFICATION_SETTINGS_ENABLED).
              |Either rename these or, only if you have an explicit reason, add them to EXCLUDED_TOGGLES_LIST.""".trimMargin(),
        ).that(invalid).isEmpty()
    }

    @Test
    fun `GIVEN stream config files WHEN compared with generated enum THEN every file is registered`() {
        // Arrange
        val configNames = CONFIG_FILES.flatMap(::parseToggles).map(ConfigToggle::name)

        // Act
        val generatedNames = FeatureToggles.entries.map(FeatureToggles::rawName)

        // Assert
        Truth.assertWithMessage(
            """Every <stream>/config-toggles/feature_toggles.json must be listed in featureTogglesStreams of core/config-toggles/build.gradle.kts.
              |Found: ${CONFIG_FILES.joinToString { it.relativeTo(REPO_ROOT).path }}""".trimMargin(),
        ).that(generatedNames).containsExactlyElementsIn(configNames)
    }

    private fun parseToggles(file: File): List<ConfigToggle> {
        val moshi = Moshi.Builder().build()
        val listType = Types.newParameterizedType(List::class.java, ConfigToggle::class.java)
        val adapter = moshi.adapter<List<ConfigToggle>>(listType)
        return requireNotNull(adapter.fromJson(file.readText())) { "Failed to parse $file" }
    }

    private companion object {
        val VALID_NAME_PATTERN = Regex("""^(AND|TWI)_\d+(?:_[A-Z0-9]+)+$""")

        /** Unit tests run from the module directory, `core/config-toggles` */
        val REPO_ROOT: File = File("../..").canonicalFile

        val CONFIG_FILES: List<File> = REPO_ROOT.listFiles().orEmpty()
            .map { File(it, "config-toggles/feature_toggles.json") }
            .filter(File::isFile)
            .sorted()

        /** Toggles created before the AND_/TWI_ naming convention. Do NOT add new entries. */
        val EXCLUDED_TOGGLES_LIST = setOf(
            "ADDRESS_SYNC_ENABLED",
            "WALLET_CONNECT_BITCOIN_ENABLED",
        )
    }
}