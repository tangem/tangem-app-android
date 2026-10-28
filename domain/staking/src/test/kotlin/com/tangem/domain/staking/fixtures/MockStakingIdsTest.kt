package com.tangem.domain.staking.fixtures

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MockStakingIdsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN integration id WHEN MockStaking constant looked up THEN equals StakingIntegrationID value`(
        id: StakingIntegrationID,
    ) {
        // Act
        val actual = mockIdOf(id)

        // Assert
        assertThat(actual).isEqualTo(id.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN integration id WHEN stakeKitBalance built THEN integration id preserved`(id: StakingIntegrationID) {
        // Act
        val actual = MockStaking.stakeKitBalance(integrationId = id.value)

        // Assert
        assertThat(
            actual.stakingId,
        ).isEqualTo(StakingID(integrationId = id.value, address = MockStaking.DELEGATOR_ADDRESS))
        assertThat(actual.balance.integrationId).isEqualTo(id.value)
    }

    private fun mockIdOf(id: StakingIntegrationID): String = when (id) {
        StakingIntegrationID.StakeKit.Coin.Solana -> MockStaking.SOLANA_ID
        StakingIntegrationID.StakeKit.Coin.Cosmos -> MockStaking.COSMOS_ID
        StakingIntegrationID.StakeKit.Coin.Tron -> MockStaking.TRON_ID
        StakingIntegrationID.StakeKit.Coin.Ton -> MockStaking.TON_ID
        StakingIntegrationID.StakeKit.Coin.BSC -> MockStaking.BSC_ID
        StakingIntegrationID.StakeKit.Coin.Cardano -> MockStaking.CARDANO_ID
        StakingIntegrationID.StakeKit.EthereumToken.Polygon -> MockStaking.POLYGON_ID
        StakingIntegrationID.P2PEthPool -> MockStaking.P2P_ID
    }

    private fun provideTestModels(): List<StakingIntegrationID> = allStakeKitIds + StakingIntegrationID.P2PEthPool
}