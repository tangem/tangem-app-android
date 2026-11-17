package com.tangem.features.polymarket.impl.featuretoggles

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()

    private val toggles = DefaultPolymarketFeatureToggles(featureTogglesManager)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN toggles WHEN crypto up down is read THEN it is on only with phase two and fast orders`(
        model: CryptoUpDownModel,
    ) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1700_POLYMARKET_PHASE_TWO_ENABLED)
        } returns model.isPhaseTwoOn
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1881_POLYMARKET_FAST_ORDERS_ENABLED)
        } returns model.isFastOrdersOn
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1882_POLYMARKET_CRYPTO_UP_DOWN_ENABLED)
        } returns model.isCryptoUpDownOn

        // Act
        val actual = toggles.isPolymarketCryptoUpDownEnabled

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class CryptoUpDownModel(
        val isPhaseTwoOn: Boolean,
        val isFastOrdersOn: Boolean,
        val isCryptoUpDownOn: Boolean,
        val expected: Boolean,
    )

    private fun provideTestModels() = listOf(
        CryptoUpDownModel(isPhaseTwoOn = true, isFastOrdersOn = true, isCryptoUpDownOn = true, expected = true),
        CryptoUpDownModel(isPhaseTwoOn = false, isFastOrdersOn = true, isCryptoUpDownOn = true, expected = false),
        CryptoUpDownModel(isPhaseTwoOn = true, isFastOrdersOn = false, isCryptoUpDownOn = true, expected = false),
        CryptoUpDownModel(isPhaseTwoOn = true, isFastOrdersOn = true, isCryptoUpDownOn = false, expected = false),
    )
}