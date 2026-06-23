package com.tangem.data.staking.toggles

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.staking.model.StakingIntegrationID
import com.google.common.truth.Truth.assertThat
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultStakingFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val toggles = DefaultStakingFeatureToggles(featureTogglesManager = featureTogglesManager)

    @BeforeEach
    fun resetMocks() {
        clearMocks(featureTogglesManager)
    }

    @Test
    fun `P2PEthPool integration is always enabled`() {
        assertThat(toggles.isIntegrationEnabled(StakingIntegrationID.P2PEthPool)).isTrue()

        verify(exactly = 0) { featureTogglesManager.isFeatureEnabled(any()) }
    }

    @Test
    fun `existing StakeKit Coin integrations are always enabled`() {
        StakingIntegrationID.StakeKit.Coin.entries.forEach { coin ->
            assertThat(toggles.isIntegrationEnabled(coin)).isTrue()
        }

        verify(exactly = 0) { featureTogglesManager.isFeatureEnabled(any()) }
    }

    @Test
    fun `existing StakeKit EthereumToken integrations are always enabled`() {
        StakingIntegrationID.StakeKit.EthereumToken.entries.forEach { token ->
            assertThat(toggles.isIntegrationEnabled(token)).isTrue()
        }

        verify(exactly = 0) { featureTogglesManager.isFeatureEnabled(any()) }
    }

    @Test
    fun `GIVEN AND_15718_STAKING_TRANSACTION_VALIDATION enabled WHEN isTransactionValidationEnabled THEN true`() {
        // Arrange
        every { featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15718_STAKING_TRANSACTION_VALIDATION) } returns true

        // Act & Assert
        assertThat(toggles.isTransactionValidationEnabled).isTrue()
        verify(exactly = 1) { featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15718_STAKING_TRANSACTION_VALIDATION) }
    }

    @Test
    fun `GIVEN AND_15718_STAKING_TRANSACTION_VALIDATION disabled WHEN isTransactionValidationEnabled THEN false`() {
        // Arrange
        every { featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15718_STAKING_TRANSACTION_VALIDATION) } returns false

        // Act & Assert
        assertThat(toggles.isTransactionValidationEnabled).isFalse()
        verify(exactly = 1) { featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15718_STAKING_TRANSACTION_VALIDATION) }
    }
}