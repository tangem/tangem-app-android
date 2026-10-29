package com.tangem.data.nft

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultNFTFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()

    private val toggles = DefaultNFTFeatureToggles(featureTogglesManager = featureTogglesManager)

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `GIVEN pNFT and Core toggle WHEN isSolanaPnftAndCoreNftEnabled THEN follows the toggle`(isToggleOn: Boolean) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1822_SOLANA_PNFT_AND_METAPLEX_CORE_ENABLED)
        } returns isToggleOn

        // Act
        val actual = toggles.isSolanaPnftAndCoreNftEnabled

        // Assert
        assertThat(actual).isEqualTo(isToggleOn)
    }
}