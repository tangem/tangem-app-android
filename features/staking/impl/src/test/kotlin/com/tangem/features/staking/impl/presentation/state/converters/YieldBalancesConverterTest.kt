package com.tangem.features.staking.impl.presentation.state.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.PendingActionConstraints
import com.tangem.domain.models.staking.RewardBlockType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingBalanceEntry
import com.tangem.domain.models.staking.StakingEntryType
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.p2pIntegration
import com.tangem.features.staking.impl.fixtures.polygonToken
import com.tangem.features.staking.impl.fixtures.stakeKitEntry
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.Provider
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class YieldBalancesConverterTest {

    private val claim = MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS)

    @Nested
    inner class P2PEthPool {

        private val ethereum = coin(Blockchain.Ethereum)

        @Test
        fun `GIVEN p2p balance with earned rewards WHEN convert THEN ethereum earned rewards not actionable`() {
            // Arrange
            val balance = MockStaking.p2pBalance(accounts = listOf(MockStaking.p2pAccount(earned = BigDecimal("0.5"))))

            // Act
            val actual = convert(ethereum, balance, integration = p2pIntegration()) as InnerYieldBalanceState.Data

            // Assert
            assertThat(actual.reward.rewardBlockType).isEqualTo(RewardBlockType.EthereumEarnedRewards)
            assertThat(actual.isActionable).isFalse()
            assertThat(actual.integrationId).isEqualTo(balance.stakingId.integrationId)
            assertThat(actual.reward.rewardConstraints).isNull()
        }

        @Test
        fun `GIVEN p2p balance without earned rewards WHEN convert THEN default reward unavailable`() {
            // Arrange
            val balance = MockStaking.p2pBalance(accounts = listOf(MockStaking.p2pAccount(earned = BigDecimal.ZERO)))

            // Act
            val actual = convert(ethereum, balance, integration = p2pIntegration()) as InnerYieldBalanceState.Data

            // Assert
            assertThat(actual.reward.rewardBlockType)
                .isEqualTo(RewardBlockType.RewardUnavailable.DefaultRewardUnavailable)
        }

        @Test
        fun `GIVEN p2p balance WHEN convert THEN rewards formatted from total rewards`() {
            // Arrange
            val earned = BigDecimal("0.5")
            val balance = MockStaking.p2pBalance(accounts = listOf(MockStaking.p2pAccount(earned = earned)))

            // Act
            val actual = convert(ethereum, balance, integration = p2pIntegration()) as InnerYieldBalanceState.Data

            // Assert
            assertThat(actual.reward.rewardsCrypto).isEqualTo(earned.format { crypto(ethereum) })
            assertThat(actual.reward.rewardsFiat).isEqualTo(fiatText(earned * BigDecimal("2")))
        }
    }

    @Nested
    inner class Solana {

        @Test
        fun `GIVEN solana actionable rewards WHEN convert THEN solana reward unavailable`() {
            // Arrange
            val balance = stakeKitBalance(MockStaking.SOLANA_ID, MockStaking.solToken)

            // Act
            val actual = convert(coin(Blockchain.Solana), balance, integration = stakeKitIntegration())

            // Assert
            actual as InnerYieldBalanceState.Data
            assertThat(actual.reward.rewardBlockType)
                .isEqualTo(RewardBlockType.RewardUnavailable.SolanaRewardUnavailable)
            assertThat(actual.isActionable).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BscAndTon {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN chain with unavailable rewards WHEN convert THEN default reward unavailable`(model: ChainModel) {
            // Arrange
            val balance = stakeKitBalance(model.id.value, model.token)

            // Act
            val actual = convert(coin(model.blockchain), balance, integration = stakeKitIntegration(model.id))

            // Assert
            actual as InnerYieldBalanceState.Data
            assertThat(actual.reward.rewardBlockType)
                .isEqualTo(RewardBlockType.RewardUnavailable.DefaultRewardUnavailable)
        }

        private fun provideTestModels() = listOf(
            ChainModel(Blockchain.BSC, StakingIntegrationID.StakeKit.Coin.BSC, MockStaking.bnbToken),
            ChainModel(Blockchain.TON, StakingIntegrationID.StakeKit.Coin.Ton, MockStaking.tonToken),
        )
    }

    @Nested
    inner class Cardano {

        private val cardano = coin(Blockchain.Cardano)
        private val integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Cardano)

        @Test
        fun `GIVEN cardano without rewards WHEN convert THEN cardano no rewards`() {
            // Arrange
            val balance = MockStaking.stakeKitBalance(
                items = listOf(item(BalanceType.STAKED, "10", MockStaking.adaToken)),
                integrationId = MockStaking.CARDANO_ID,
            )

            // Act
            val actual = convert(cardano, balance, integration = integration) as InnerYieldBalanceState.Data

            // Assert
            assertThat(actual.reward.rewardBlockType).isEqualTo(RewardBlockType.CardanoNoRewards)
            assertThat(actual.isActionable).isFalse()
        }

        @Test
        fun `GIVEN cardano actionable rewards WHEN convert THEN rewards`() {
            // Arrange
            val balance = MockStaking.stakeKitBalance(
                items = listOf(item(BalanceType.REWARDS, "0.5", MockStaking.adaToken, listOf(claim))),
                integrationId = MockStaking.CARDANO_ID,
            )

            // Act
            val actual = convert(cardano, balance, integration = integration) as InnerYieldBalanceState.Data

            // Assert
            assertThat(actual.reward.rewardBlockType).isEqualTo(RewardBlockType.Rewards)
            assertThat(actual.isActionable).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Generic {

        private val cosmos = coin(Blockchain.Cosmos)
        private val cosmosIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Cosmos)

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN rewards with pending actions WHEN convert THEN rewards actionable`(model: GenericModel) {
            // Arrange
            val balance = MockStaking.stakeKitBalance(
                items = listOf(item(BalanceType.REWARDS, "0.5", model.token, listOf(claim))),
                integrationId = model.integration.integrationId.value,
            )

            // Act
            val actual = convert(model.currency, balance, integration = model.integration)

            // Assert
            actual as InnerYieldBalanceState.Data
            assertThat(actual.reward.rewardBlockType).isEqualTo(RewardBlockType.Rewards)
            assertThat(actual.isActionable).isTrue()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN rewards without pending actions WHEN convert THEN rewards requirements error actionable`(
            model: GenericModel,
        ) {
            // Arrange
            val balance = MockStaking.stakeKitBalance(
                items = listOf(item(BalanceType.REWARDS, "0.5", model.token)),
                integrationId = model.integration.integrationId.value,
            )

            // Act
            val actual = convert(model.currency, balance, integration = model.integration)

            // Assert
            actual as InnerYieldBalanceState.Data
            assertThat(actual.reward.rewardBlockType).isEqualTo(RewardBlockType.RewardsRequirementsError)
            assertThat(actual.isActionable).isTrue()
        }

        @Test
        fun `GIVEN zero rewards with actions WHEN convert THEN no rewards`() {
            // Arrange
            val balance = cosmosBalance(item(BalanceType.REWARDS, "0", MockStaking.atomToken, listOf(claim)))

            // Act
            val actual = convert(cosmos, balance, integration = cosmosIntegration) as InnerYieldBalanceState.Data

            // Assert
            assertThat(actual.reward.rewardBlockType).isEqualTo(RewardBlockType.NoRewards)
            assertThat(actual.isActionable).isFalse()
        }

        @Test
        fun `GIVEN no rewards items WHEN convert THEN no rewards`() {
            // Arrange
            val balance = cosmosBalance(item(BalanceType.STAKED, "5", MockStaking.atomToken))

            // Act
            val actual = convert(cosmos, balance, integration = cosmosIntegration) as InnerYieldBalanceState.Data

            // Assert
            assertThat(actual.reward.rewardBlockType).isEqualTo(RewardBlockType.NoRewards)
        }

        @Test
        fun `GIVEN rewards with claim constraint WHEN convert THEN constraint exposed`() {
            // Arrange
            val constraint = constraint(StakingActionType.CLAIM_REWARDS)
            val balance = cosmosBalance(
                item(BalanceType.REWARDS, "0.5", MockStaking.atomToken).copy(
                    pendingActionsConstraints = listOf(constraint),
                ),
            )

            // Act
            val actual = convert(cosmos, balance, integration = cosmosIntegration) as InnerYieldBalanceState.Data

            // Assert
            assertThat(actual.reward.rewardConstraints).isEqualTo(constraint)
        }

        @Test
        fun `GIVEN rewards with non claim constraint only WHEN convert THEN no constraint`() {
            // Arrange
            val balance = cosmosBalance(
                item(BalanceType.REWARDS, "0.5", MockStaking.atomToken).copy(
                    pendingActionsConstraints = listOf(constraint(StakingActionType.RESTAKE_REWARDS)),
                ),
            )

            // Act
            val actual = convert(cosmos, balance, integration = cosmosIntegration) as InnerYieldBalanceState.Data

            // Assert
            assertThat(actual.reward.rewardConstraints).isNull()
        }

        private fun constraint(type: StakingActionType) = PendingActionConstraints(
            type = type,
            amountArg = PendingAction.PendingActionArgs.Amount(
                isRequired = true,
                minimum = BigDecimal.ONE,
                maximum = null,
            ),
        )

        private fun provideTestModels() = listOf(
            GenericModel(cosmos, cosmosIntegration, MockStaking.atomToken),
            GenericModel(
                currency = polygonToken(),
                integration = stakeKitIntegration(StakingIntegrationID.StakeKit.EthereumToken.Polygon),
                token = MockStaking.polToken,
            ),
        )
    }

    @Nested
    inner class EmptyAndPending {

        private val cosmos = coin(Blockchain.Cosmos)
        private val cosmosIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Cosmos)

        @Test
        fun `GIVEN no staking balance and no pending entries WHEN convert THEN empty`() {
            // Act
            val actual = convert(
                currency = cosmos,
                stakingBalance = null,
                entries = listOf(stakeKitEntry(StakingEntryType.STAKED, BigDecimal("5"))),
                integration = cosmosIntegration,
            )

            // Assert
            assertThat(actual).isEqualTo(InnerYieldBalanceState.Empty)
        }

        @Test
        fun `GIVEN no staking balance and pending entry WHEN convert THEN data without integration id`() {
            // Act
            val actual = convert(
                currency = cosmos,
                stakingBalance = null,
                entries = listOf(stakeKitEntry(StakingEntryType.STAKED, BigDecimal("5"), isPending = true)),
                integration = cosmosIntegration,
            )

            // Assert
            actual as InnerYieldBalanceState.Data
            assertThat(actual.integrationId).isNull()
            assertThat(actual.reward.rewardBlockType).isEqualTo(RewardBlockType.NoRewards)
            assertThat(actual.reward.rewardsCrypto).isEqualTo((null as BigDecimal?).format { crypto(cosmos) })
            assertThat(actual.balances.single().isPending).isTrue()
        }
    }

    @Nested
    inner class Balances {

        private val cosmos = coin(Blockchain.Cosmos)
        private val cosmosIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Cosmos)
        private val balance = MockStaking.stakeKitBalance(
            items = listOf(MockStaking.balanceItem(token = MockStaking.atomToken)),
            integrationId = MockStaking.COSMOS_ID,
        )

        @Test
        fun `GIVEN zero amount and rewards entries WHEN convert THEN both filtered out`() {
            // Arrange
            val entries = listOf(
                stakeKitEntry(StakingEntryType.STAKED, BigDecimal.ZERO, id = "zero"),
                stakeKitEntry(StakingEntryType.REWARDS, BigDecimal.ONE, id = "rewards"),
                stakeKitEntry(StakingEntryType.STAKED, BigDecimal("2"), id = "staked"),
            )

            // Act
            val actual = convertBalances(entries)

            // Assert
            assertThat(actual.map { it.groupId }).containsExactly("staked")
            assertThat(actual.single().cryptoAmount).isEquivalentAccordingToCompareTo(BigDecimal("2"))
        }

        @Test
        fun `GIVEN entries without title WHEN convert THEN dropped`() {
            // Arrange
            val entries = listOf(
                stakeKitEntry(StakingEntryType.AVAILABLE, BigDecimal.ONE, id = "available"),
                stakeKitEntry(StakingEntryType.UNLOCKING, BigDecimal.ONE, id = "unlocking"),
                stakeKitEntry(StakingEntryType.STAKED, BigDecimal("2"), id = "staked"),
            )

            // Act
            val actual = convertBalances(entries)

            // Assert
            assertThat(actual.map { it.type }).containsExactly(BalanceType.STAKED)
        }

        @Test
        fun `GIVEN mixed entries WHEN convert THEN sorted by type order then amount descending`() {
            // Arrange
            val entries = listOf(
                stakeKitEntry(StakingEntryType.UNSTAKING, BigDecimal("5"), id = "u"),
                stakeKitEntry(StakingEntryType.STAKED, BigDecimal("1"), id = "s1"),
                stakeKitEntry(StakingEntryType.STAKED, BigDecimal("3"), id = "s3"),
                stakeKitEntry(StakingEntryType.WITHDRAWABLE, BigDecimal("9"), id = "w"),
                stakeKitEntry(StakingEntryType.LOCKED, BigDecimal("2"), id = "l"),
            )

            // Act
            val actual = convertBalances(entries)

            // Assert
            assertThat(actual.map { it.groupId }).containsExactly("s3", "s1", "l", "u", "w").inOrder()
        }

        private fun convertBalances(entries: List<StakingBalanceEntry>) =
            (convert(cosmos, balance, entries, cosmosIntegration) as InnerYieldBalanceState.Data).balances
    }

    private fun stakeKitBalance(integrationId: String, token: YieldToken) = MockStaking.stakeKitBalance(
        items = listOf(
            item(BalanceType.STAKED, "5", token),
            item(BalanceType.REWARDS, "0.5", token, listOf(claim)),
        ),
        integrationId = integrationId,
    )

    private fun cosmosBalance(vararg items: BalanceItem) =
        MockStaking.stakeKitBalance(items = items.toList(), integrationId = MockStaking.COSMOS_ID)

    private fun item(
        type: BalanceType,
        amount: String,
        token: YieldToken,
        pendingActions: List<PendingAction> = emptyList(),
    ) = MockStaking.balanceItem(
        type = type,
        amount = BigDecimal(amount),
        token = token,
        pendingActions = pendingActions,
    )

    private fun fiatText(value: BigDecimal) = value.format {
        fiat(fiatCurrencyCode = AppCurrency.Default.code, fiatCurrencySymbol = AppCurrency.Default.symbol)
    }

    private fun convert(
        currency: CryptoCurrency,
        stakingBalance: StakingBalance?,
        entries: List<StakingBalanceEntry> = emptyList(),
        integration: StakingIntegration,
        fiatRate: BigDecimal? = BigDecimal("2"),
    ) = YieldBalancesConverter(
        cryptoCurrencyStatus = currencyStatus(currency, BigDecimal("100"), fiatRate, stakingBalance),
        appCurrencyProvider = Provider { AppCurrency.Default },
        balancesToShowProvider = Provider { entries },
        integration = integration,
    ).convert(Unit)

    internal data class ChainModel(
        val blockchain: Blockchain,
        val id: StakingIntegrationID.StakeKit,
        val token: YieldToken,
    )

    internal data class GenericModel(
        val currency: CryptoCurrency,
        val integration: StakingIntegration,
        val token: YieldToken,
    )
}