package com.tangem.tap.domain.tokens

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins which toggle gates the balance-contributions migration. The manager mock is deliberately NOT relaxed, so
 * reading any toggle other than [FeatureToggles.TWI_1717_BALANCE_CONTRIBUTIONS] fails the test outright.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTokensFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()

    private val toggles = DefaultTokensFeatureToggles(featureTogglesManager = featureTogglesManager)

    @BeforeEach
    fun resetMocks() {
        clearMocks(featureTogglesManager)
    }

    @Test
    fun `GIVEN TWI_1717_BALANCE_CONTRIBUTIONS enabled WHEN isBalanceContributionsEnabled THEN true`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1717_BALANCE_CONTRIBUTIONS)
        } returns true

        // Act & Assert
        assertThat(toggles.isBalanceContributionsEnabled).isTrue()
        verify(exactly = 1) {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1717_BALANCE_CONTRIBUTIONS)
        }
    }

    @Test
    fun `GIVEN TWI_1717_BALANCE_CONTRIBUTIONS disabled WHEN isBalanceContributionsEnabled THEN false`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1717_BALANCE_CONTRIBUTIONS)
        } returns false

        // Act & Assert
        assertThat(toggles.isBalanceContributionsEnabled).isFalse()
        verify(exactly = 1) {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1717_BALANCE_CONTRIBUTIONS)
        }
    }

    @Test
    fun `GIVEN the toggle WHEN isBalanceContributionsEnabled read twice THEN it is re-evaluated each time`() {
        // Arrange — the property has a getter, not a cached value: flipping the toggle at runtime must be observed
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1717_BALANCE_CONTRIBUTIONS)
        } returnsMany listOf(false, true)

        // Act & Assert
        assertThat(toggles.isBalanceContributionsEnabled).isFalse()
        assertThat(toggles.isBalanceContributionsEnabled).isTrue()
    }
}