package com.tangem.core.configtoggle.feature

import com.google.common.truth.Truth
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.core.configtoggle.storage.ConfigToggle
import org.junit.jupiter.api.Test
import java.io.File

internal class FeatureTogglesNamingConventionTest {

    @Test
    fun `all new feature toggles must follow AND_id or TWI_id naming`() {
        val toggles = parseToggles(CONFIG_FILE)

        val invalid = toggles
            .map(ConfigToggle::name)
            .filterNot { it in EXCLUDED_TOGGLES_LIST }
            .filterNot(VALID_NAME_PATTERN::matches)

        Truth.assertWithMessage(
            """New feature toggles must match pattern ${VALID_NAME_PATTERN.pattern} — AND_<ticket_id> (Android ticket, e.g. AND_15312_PUSH_NOTIFICATION_SETTINGS_ENABLED) or TWI_<ticket_id> (idea ticket, e.g. TWI_1403_PUSH_NOTIFICATION_SETTINGS_ENABLED).
              |Either rename these or, only if you have an explicit reason, add them to LEGACY_EXCLUDED.""".trimMargin(),
        ).that(invalid).isEmpty()
    }

    private fun parseToggles(file: File): List<ConfigToggle> {
        val moshi = Moshi.Builder().build()
        val listType = Types.newParameterizedType(List::class.java, ConfigToggle::class.java)
        val adapter = moshi.adapter<List<ConfigToggle>>(listType)
        return requireNotNull(adapter.fromJson(file.readText())) { "Failed to parse $file" }
    }

    private companion object {
        val VALID_NAME_PATTERN = Regex("""^(AND|TWI)_\d+(?:_[A-Z0-9]+)+$""")

        val CONFIG_FILE = File("src/main/assets/configs/feature_toggles_config.json")

        /** Toggles created before the AND_/TWI_ naming convention. Do NOT add new entries. */
        val EXCLUDED_TOGGLES_LIST = setOf(
            "ADDRESS_SYNC_ENABLED",
            "APP_REDESIGN_ENABLED",
            "ASSETS_DISCOVERY_ENABLED",
            "DYNAMIC_ADDRESSES_ENABLED",
            "HEDERA_ERC20_ENABLED",
            "NEW_CARD_SCANNING_ENABLED",
            "SOLANA_SCALED_UI_AMOUNT_ENABLED",
            "SOLANA_TX_HISTORY_ENABLED",
            "STAKING_ETH_ENABLED",
            "SWAP_AB_ENABLED",
            "VIRTUAL_ACCOUNTS_ENABLED",
            "VISA_ONBOARDING_ENABLED",
            "WALLET_CONNECT_BITCOIN_ENABLED",
        )
    }
}