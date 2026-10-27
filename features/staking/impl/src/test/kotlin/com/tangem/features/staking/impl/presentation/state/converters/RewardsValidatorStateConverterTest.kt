package com.tangem.features.staking.impl.presentation.state.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.BalanceItem
import com.tangem.domain.staking.model.BalanceType
import com.tangem.domain.staking.model.PendingAction
import com.tangem.domain.staking.model.action.StakingActionType
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.p2pIntegration
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class RewardsValidatorStateConverterTest {

    private val jailedTarget = StakingTarget.Validator(
        Yield.Validator(
            address = JAILED_ADDRESS,
            status = Yield.Validator.ValidatorStatus.JAILED,
            name = "Jailed validator",
            isPreferred = false,
            isStrategicPartner = false,
        ),
    )

    private val listedTarget = StakingTarget.Validator(
        Yield.Validator(
            address = LISTED_ADDRESS,
            status = Yield.Validator.ValidatorStatus.ACTIVE,
            name = "Listed validator",
            isPreferred = true,
            isStrategicPartner = false,
        ),
    )

    private val integration: StakeKitIntegration = mockk {
        every { targets } returns listOf(listedTarget)
        every { allTargets } returns listOf(listedTarget, jailedTarget)
    }

    @Test
    fun `GIVEN rewards at jailed and unlisted validators WHEN convert THEN both rows keep their own validator`() {
        // Arrange
        val converter = createConverter(
            createRewardItem(validatorAddress = JAILED_ADDRESS),
            createRewardItem(validatorAddress = UNKNOWN_ADDRESS),
        )

        // Act
        val actual = converter.convert(Unit) as StakingStates.RewardsValidatorsState.Data

        // Assert
        assertThat(actual.rewards.map { it.target?.address }).containsExactly(JAILED_ADDRESS, UNKNOWN_ADDRESS).inOrder()
        assertThat(actual.rewards.first().target).isEqualTo(jailedTarget)
        assertThat(actual.rewards.last().target?.isPreferred).isFalse()
    }

    @Test
    fun `GIVEN claimable reward without validator address WHEN convert THEN row is kept at first listed validator`() {
        // Arrange
        val claimAction = PendingAction(type = StakingActionType.CLAIM_REWARDS, passthrough = "claim", args = null)
        val converter = createConverter(createRewardItem(validatorAddress = null, pendingActions = listOf(claimAction)))

        // Act
        val actual = converter.convert(Unit) as StakingStates.RewardsValidatorsState.Data

        // Assert
        assertThat(actual.rewards.map { it.target }).containsExactly(listedTarget)
        assertThat(actual.rewards.single().pendingActions).containsExactly(claimAction)
    }

    @Test
    fun `GIVEN reward without validator address and no listed validators WHEN convert THEN row dropped`() {
        // Arrange
        val noTargetsIntegration: StakeKitIntegration = mockk {
            every { targets } returns emptyList()
            every { allTargets } returns emptyList()
        }
        val converter = createConverter(createRewardItem(validatorAddress = null), integration = noTargetsIntegration)

        // Act
        val actual = converter.convert(Unit)

        // Assert
        assertThat(actual).isEqualTo(
            StakingStates.RewardsValidatorsState.Data(isPrimaryButtonEnabled = true, rewards = persistentListOf()),
        )
    }

    @Test
    fun `GIVEN currency without raw id WHEN convert THEN row has no raw currency id`() {
        // Arrange
        val currency: CryptoCurrency = mockk(relaxed = true) { every { id.rawCurrencyId } returns null }
        val converter = createConverter(createRewardItem(validatorAddress = LISTED_ADDRESS), currency = currency)

        // Act
        val actual = converter.convert(Unit) as StakingStates.RewardsValidatorsState.Data

        // Assert
        assertThat(actual.rewards.single().rawCurrencyId).isNull()
    }

    @Nested
    inner class Variants {

        private val cosmos = coin(Blockchain.Cosmos)
        private val cosmosIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Cosmos)

        @Test
        fun `GIVEN p2p balance WHEN convert THEN empty state`() {
            // Arrange
            val converter = converter(stakingBalance = MockStaking.p2pBalance(), integration = p2pIntegration())

            // Act
            val actual = converter.convert(Unit)

            // Assert
            assertThat(actual).isEqualTo(StakingStates.RewardsValidatorsState.Empty())
        }

        @Test
        fun `GIVEN no staking balance WHEN convert THEN empty state`() {
            // Act
            val actual = converter(stakingBalance = null).convert(Unit)

            // Assert
            assertThat(actual).isEqualTo(StakingStates.RewardsValidatorsState.Empty())
        }

        @Test
        fun `GIVEN staked and rewards items WHEN convert THEN only rewards rows`() {
            // Arrange
            val converter = converter(
                stakingBalance = cosmosBalance(
                    MockStaking.balanceItem(type = BalanceType.STAKED, amount = BigDecimal("5")),
                    MockStaking.balanceItem(type = BalanceType.REWARDS, amount = BigDecimal("0.5"), groupId = "r"),
                ),
            )

            // Act
            val actual = converter.convert(Unit) as StakingStates.RewardsValidatorsState.Data

            // Assert
            assertThat(actual.isPrimaryButtonEnabled).isTrue()
            assertThat(actual.rewards.map { it.groupId }).containsExactly("r")
        }

        @Test
        fun `GIVEN no rewards items WHEN convert THEN data with empty rewards`() {
            // Arrange
            val converter = converter(
                stakingBalance = cosmosBalance(MockStaking.balanceItem(type = BalanceType.STAKED)),
            )

            // Act
            val actual = converter.convert(Unit)

            // Assert
            assertThat(actual).isEqualTo(
                StakingStates.RewardsValidatorsState.Data(isPrimaryButtonEnabled = true, rewards = persistentListOf()),
            )
        }

        @Test
        fun `GIVEN reward item WHEN convert THEN row carries amounts and passthrough fields`() {
            // Arrange
            val claim = MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS)
            val converter = converter(
                stakingBalance = cosmosBalance(
                    MockStaking.balanceItem(
                        type = BalanceType.REWARDS,
                        amount = BigDecimal("0.5"),
                        pendingActions = listOf(claim),
                        isPending = true,
                    ),
                ),
            )

            // Act
            val row = (converter.convert(Unit) as StakingStates.RewardsValidatorsState.Data).rewards.single()

            // Assert
            assertThat(row.title).isEqualTo(stringReference("Validator 1"))
            assertThat(row.subtitle).isNull()
            assertThat(row.cryptoAmount).isEquivalentAccordingToCompareTo(BigDecimal("0.5"))
            assertThat(row.fiatAmount).isEquivalentAccordingToCompareTo(BigDecimal("1.0"))
            assertThat(row.isClickable).isTrue()
            assertThat(row.type).isEqualTo(BalanceType.REWARDS)
            assertThat(row.isPending).isTrue()
            assertThat(row.targetAddress).isEqualTo(MockStaking.VALIDATOR_ADDRESS)
            assertThat(row.pendingActions).containsExactly(claim)
            assertThat(row.rawCurrencyId).isEqualTo(cosmos.id.rawCurrencyId?.value)
        }

        @Test
        fun `GIVEN no fiat rate WHEN convert THEN fiat amount null`() {
            // Arrange
            val converter = converter(
                stakingBalance = cosmosBalance(MockStaking.balanceItem(type = BalanceType.REWARDS)),
                fiatRate = null,
            )

            // Act
            val row = (converter.convert(Unit) as StakingStates.RewardsValidatorsState.Data).rewards.single()

            // Assert
            assertThat(row.fiatAmount).isNull()
        }

        private fun cosmosBalance(vararg items: BalanceItem) =
            MockStaking.stakeKitBalance(items = items.toList(), integrationId = MockStaking.COSMOS_ID)

        private fun converter(
            stakingBalance: StakingBalance?,
            integration: StakingIntegration = cosmosIntegration,
            fiatRate: BigDecimal? = BigDecimal("2"),
        ) = RewardsValidatorStateConverter(
            cryptoCurrencyStatus = currencyStatus(cosmos, fiatRate = fiatRate, stakingBalance = stakingBalance),
            appCurrencyProvider = Provider { AppCurrency.Default },
            integration = integration,
        )
    }

    private fun createConverter(
        vararg items: BalanceItem,
        integration: StakingIntegration = this.integration,
        currency: CryptoCurrency = mockk(relaxed = true),
    ): RewardsValidatorStateConverter {
        val stakingBalance: StakingBalance.Data.StakeKit = mockk {
            every { balance } returns mockk { every { this@mockk.items } returns items.toList() }
        }
        return RewardsValidatorStateConverter(
            cryptoCurrencyStatus = CryptoCurrencyStatus(
                currency = currency,
                value = mockk(relaxed = true) {
                    every { this@mockk.fiatRate } returns null
                    every { this@mockk.contributions } returns listOf(stakingBalance)
                },
            ),
            appCurrencyProvider = Provider { AppCurrency.Default },
            integration = integration,
        )
    }

    private fun createRewardItem(validatorAddress: String?, pendingActions: List<PendingAction> = emptyList()) =
        BalanceItem(
            groupId = "group-$validatorAddress",
            token = mockk(relaxed = true),
            type = BalanceType.REWARDS,
            amount = BigDecimal.ONE,
            rawCurrencyId = null,
            validatorAddress = validatorAddress,
            date = null,
            pendingActions = pendingActions,
            pendingActionsConstraints = emptyList(),
            isPending = false,
        )

    private companion object {
        const val LISTED_ADDRESS = "cosmosvaloper1listed"
        const val JAILED_ADDRESS = "cosmosvaloper1jailed"
        const val UNKNOWN_ADDRESS = "cosmosvaloper1unknownwxyz"
    }
}