package com.tangem.test.mock.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class MockStakingTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldFixture {

        @Test
        fun `GIVEN default args WHEN yield THEN single active preferred validator and amount argument`() {
            // Act
            val actual = MockStaking.yield()

            // Assert
            assertThat(actual.validators).containsExactly(
                Yield.Validator(
                    address = MockStaking.VALIDATOR_ADDRESS,
                    status = Yield.Validator.ValidatorStatus.ACTIVE,
                    name = "Validator 1",
                    image = null,
                    website = null,
                    rewardInfo = RewardInfo(
                        BigDecimal("0.05"),
                        RewardType.APR,
                    ),
                    commission = null,
                    isPreferred = true,
                    isStrategicPartner = false,
                ),
            )
            assertThat(actual.preferredValidators).isEqualTo(actual.validators)
            assertThat(actual.args.enter.args[Yield.Args.ArgType.AMOUNT]).isEqualTo(
                MockStaking.amountArgument(minimum = BigDecimal.ONE, maximum = null, required = true),
            )
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN per chain preset WHEN read THEN id and token match the chain`(model: PresetModel) {
            // Act
            val actual = model.preset

            // Assert
            assertThat(Triple(actual.id, actual.token.symbol, actual.token.network))
                .isEqualTo(Triple(model.id, model.symbol, model.network))
            assertThat(actual.metadata.token).isEqualTo(actual.token)
            assertThat(MockStaking.yieldFor(model.id)).isSameInstanceAs(model.preset)
        }

        @Test
        fun `GIVEN unknown id WHEN yieldFor THEN throws IllegalStateException`() {
            // Act
            val error = runCatching { MockStaking.yieldFor("unknown") }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
        }

        private fun provideTestModels() = listOf(
            PresetModel(MockStaking.SOLANA_ID, "SOL", NetworkType.SOLANA, MockStaking.solanaYield),
            PresetModel(MockStaking.COSMOS_ID, "ATOM", NetworkType.COSMOS, MockStaking.cosmosYield),
            PresetModel(MockStaking.TRON_ID, "TRX", NetworkType.TRON, MockStaking.tronYield),
            PresetModel(MockStaking.TON_ID, "TON", NetworkType.TON, MockStaking.tonYield),
            PresetModel(MockStaking.BSC_ID, "BNB", NetworkType.BINANCE, MockStaking.bscYield),
            PresetModel(MockStaking.CARDANO_ID, "ADA", NetworkType.CARDANO, MockStaking.cardanoYield),
            PresetModel(MockStaking.POLYGON_ID, "POL", NetworkType.ETHEREUM, MockStaking.polygonYield),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BalanceFixtures {

        @Test
        fun `GIVEN default p2p account WHEN p2pBalance THEN totals follow the account`() {
            // Act
            val actual = MockStaking.p2pBalance()

            // Assert
            assertThat(actual.totalStaked).isEqualToIgnoringScale(BigDecimal.ONE)
            assertThat(actual.totalRewards).isEqualToIgnoringScale(BigDecimal.ZERO)
            assertThat(actual.stakingId).isEqualTo(MockStaking.stakingId(MockStaking.P2P_ID))
        }

        @Test
        fun `GIVEN staked item WHEN stakeKitBalance THEN totalStaked equals item amount`() {
            // Act
            val actual = MockStaking.stakeKitBalance(
                items = listOf(MockStaking.balanceItem(amount = BigDecimal("2.5"))),
            )

            // Assert
            assertThat(actual.totalStaked).isEqualToIgnoringScale(BigDecimal("2.5"))
        }
    }

    data class PresetModel(
        val id: String,
        val symbol: String,
        val network: NetworkType,
        val preset: Yield,
    ) {
        override fun toString(): String = id
    }
}