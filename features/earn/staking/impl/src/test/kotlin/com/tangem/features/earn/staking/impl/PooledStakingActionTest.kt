package com.tangem.features.earn.staking.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.features.earn.staking.impl.fixtures.coin
import org.junit.jupiter.api.Test

internal class PooledStakingActionTest {

    @Test
    fun `GIVEN wallet and ETH coin WHEN stakingRoute THEN AppRoute Staking with P2PEthPool integration`() {
        // Arrange
        val userWalletId = UserWalletId("1234567890ABCDEF")
        val currency = coin(Blockchain.Ethereum)

        // Act
        val actual = stakingRoute(userWalletId, currency)

        // Assert
        assertThat(actual).isEqualTo(AppRoute.Staking(userWalletId, currency, StakingIntegrationID.P2PEthPool))
    }
}