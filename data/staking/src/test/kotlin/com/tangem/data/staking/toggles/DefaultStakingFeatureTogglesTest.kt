package com.tangem.data.staking.toggles

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.staking.model.StakingIntegrationID
import io.mockk.clearMocks
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
    fun `GIVEN P2PEthPool id WHEN isIntegrationEnabled THEN true without reading toggles`() {
        // Act & Assert
        assertThat(toggles.isIntegrationEnabled(StakingIntegrationID.P2PEthPool)).isTrue()

        // Assert
        verify(exactly = 0) { featureTogglesManager.isFeatureEnabled(any()) }
    }

    @Test
    fun `GIVEN every StakeKit Coin id WHEN isIntegrationEnabled THEN true without reading toggles`() {
        // Act & Assert
        StakingIntegrationID.StakeKit.Coin.entries.forEach { coin ->
            assertThat(toggles.isIntegrationEnabled(coin)).isTrue()
        }

        // Assert
        verify(exactly = 0) { featureTogglesManager.isFeatureEnabled(any()) }
    }

    @Test
    fun `GIVEN every StakeKit EthereumToken id WHEN isIntegrationEnabled THEN true without reading toggles`() {
        // Act & Assert
        StakingIntegrationID.StakeKit.EthereumToken.entries.forEach { token ->
            assertThat(toggles.isIntegrationEnabled(token)).isTrue()
        }

        // Assert
        verify(exactly = 0) { featureTogglesManager.isFeatureEnabled(any()) }
    }
}