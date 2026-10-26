package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.balanceState
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.features.staking.impl.presentation.state.target
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class SetConfirmationStateInitTransformerTest {

    private val cosmos = coin(Blockchain.Cosmos)
    private val cosmosIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Cosmos)
    private val solana = coin(Blockchain.Solana)
    private val solanaIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ActionTypeMapping {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN pending action type WHEN transform THEN mapped action type`(model: MappingModel) {
            // Act
            val result = transform(pendingActions = persistentListOf(MockStaking.pendingAction(model.type)))

            // Assert
            assertThat(observedAction(result)).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            MappingModel(StakingActionType.STAKE, StakingActionCommonType.Pending.Stake(shouldSkipEnterAmount = false)),
            MappingModel(StakingActionType.UNSTAKE, StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)),
            MappingModel(StakingActionType.CLAIM_REWARDS, StakingActionCommonType.Pending.Rewards),
            MappingModel(StakingActionType.RESTAKE_REWARDS, StakingActionCommonType.Pending.Rewards),
            MappingModel(StakingActionType.VOTE_LOCKED, StakingActionCommonType.Pending.Restake),
            MappingModel(StakingActionType.RESTAKE, StakingActionCommonType.Pending.Restake),
        ) + listOf(
            StakingActionType.WITHDRAW,
            StakingActionType.CLAIM_UNSTAKED,
            StakingActionType.UNLOCK_LOCKED,
            StakingActionType.STAKE_LOCKED,
            StakingActionType.VOTE,
            StakingActionType.REVOKE,
            StakingActionType.REVOTE,
            StakingActionType.REBOND,
            StakingActionType.MIGRATE,
            StakingActionType.UNKNOWN,
        ).map { MappingModel(it, StakingActionCommonType.Pending.Other) }
    }

    @Nested
    inner class EnterAndExit {

        @Test
        fun `GIVEN enter WHEN transform THEN enter with integration partial flag`() {
            // Act
            val result = transform(isEnter = true)

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Enter(shouldSkipEnterAmount = false))
            assertThat(observedEditable(result)).isTrue()
        }

        @Test
        fun `GIVEN enter with partial amount disabled WHEN transform THEN enter skips amount and not editable`() {
            // Act
            val result = transform(isEnter = true, integration = partialDisabled())

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Enter(shouldSkipEnterAmount = true))
            assertThat(observedEditable(result)).isFalse()
        }

        @Test
        fun `GIVEN enter and explicit exit WHEN transform THEN enter wins`() {
            // Act
            val result = transform(isEnter = true, isExplicitExit = true)

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Enter(shouldSkipEnterAmount = false))
        }

        @Test
        fun `GIVEN explicit exit WHEN transform THEN exit with exit args flag`() {
            // Act
            val result = transform(isExplicitExit = true)

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false))
            assertThat(observedEditable(result)).isTrue()
        }

        @Test
        fun `GIVEN explicit exit with exit partial disabled WHEN transform THEN exit not editable`() {
            // Act
            val result = transform(isExplicitExit = true, integration = partialDisabled())

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = true))
            assertThat(observedEditable(result)).isFalse()
        }

        @Test
        fun `GIVEN empty pending actions WHEN transform THEN implicit exit`() {
            // Act
            val result = transform(pendingActions = persistentListOf())

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false))
        }

        // Pins current behaviour: a null list is not treated as an implicit exit, unlike an empty one.
        @Test
        fun `GIVEN null pending actions WHEN transform THEN pending other`() {
            // Act
            val result = transform(pendingActions = null, pendingAction = null)

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Pending.Other)
        }

        @Test
        fun `GIVEN tron revote WHEN transform THEN implicit exit`() {
            // Act
            val result = transform(
                currency = coin(Blockchain.Tron),
                integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Tron),
                pendingActions = persistentListOf(MockStaking.pendingAction(StakingActionType.REVOTE)),
            )

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false))
        }

        @Test
        fun `GIVEN pending stake with enter partial disabled WHEN transform THEN stake skips amount`() {
            // Act
            val result = transform(
                integration = partialDisabled(),
                pendingActions = persistentListOf(MockStaking.pendingAction(StakingActionType.STAKE)),
            )

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Pending.Stake(shouldSkipEnterAmount = true))
        }
    }

    @Nested
    inner class SolanaPartialUnstake {

        @Test
        fun `GIVEN solana explicit exit at non preferred validator WHEN transform THEN partial unstake disabled`() {
            // Act
            val result = transform(
                isExplicitExit = true,
                balanceState = balanceState(target = target(MockStaking.validator(preferred = false))),
                currency = solana,
                integration = solanaIntegration,
            )

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = true))
        }

        @Test
        fun `GIVEN solana explicit exit at preferred validator WHEN transform THEN partial unstake allowed`() {
            // Act
            val result = transform(isExplicitExit = true, currency = solana, integration = solanaIntegration)

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false))
        }

        // Pins current behaviour: without a balance the target is unknown, so Solana treats it as non preferred.
        @Test
        fun `GIVEN solana explicit exit without balance state WHEN transform THEN partial unstake disabled`() {
            // Act
            val result = transform(
                isExplicitExit = true,
                balanceState = null,
                currency = solana,
                integration = solanaIntegration,
            )

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = true))
            assertThat(result.balanceState).isNull()
        }

        // Pins current behaviour: the Solana rule reads the network from the previous state, not the currency.
        @Test
        fun `GIVEN solana currency but non solana prev state id WHEN transform THEN solana rule not applied`() {
            // Act
            val result = transform(
                isExplicitExit = true,
                balanceState = balanceState(target = target(MockStaking.validator(preferred = false))),
                currency = solana,
                integration = solanaIntegration,
                prevBlockchainId = Blockchain.Cosmos.toNetworkId(),
            )

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false))
        }
    }

    @Nested
    inner class ConfirmationData {

        @Test
        fun `GIVEN approval needed WHEN transform THEN confirmation approval flags`() {
            // Act
            val result = transform(
                stakingApproval = StakingApproval.Needed(spenderAddress = "0xspender"),
                stakingAllowance = BigDecimal("5"),
            )

            // Assert
            assertThat(confirmation(result).isApprovalNeeded).isTrue()
            assertThat(confirmation(result).allowance).isEqualTo(BigDecimal("5"))
        }

        @Test
        fun `GIVEN no approval WHEN transform THEN approval not needed`() {
            // Act
            val result = transform(stakingApproval = StakingApproval.Empty)

            // Assert
            assertThat(confirmation(result).isApprovalNeeded).isFalse()
        }

        @Test
        fun `GIVEN claim action WHEN transform THEN confirmation starts disabled with loading fee and selected action`() {
            // Arrange
            val claim = MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS)

            // Act
            val result = transform(pendingActions = persistentListOf(claim))

            // Assert
            val data = confirmation(result)
            assertThat(result.balanceState).isEqualTo(balanceState())
            assertThat(data.isPrimaryButtonEnabled).isFalse()
            assertThat(data.innerState).isEqualTo(InnerConfirmationStakingState.ASSENT)
            assertThat(data.feeState).isEqualTo(FeeState.Loading)
            assertThat(data.notifications).isEmpty()
            assertThat(data.pendingAction).isEqualTo(claim)
            assertThat(data.isAmountEditable).isFalse()
            assertThat(data.reduceAmountBy).isNull()
        }

        @Test
        fun `GIVEN solana composite withdraw WHEN transform THEN pending actions kept`() {
            // Arrange
            val withdraw = MockStaking.pendingAction(StakingActionType.WITHDRAW)
            val unstake = MockStaking.pendingAction(StakingActionType.UNSTAKE)

            // Act
            val result = transform(
                currency = solana,
                integration = solanaIntegration,
                pendingActions = persistentListOf(withdraw, unstake),
            )

            // Assert
            assertThat(confirmation(result).pendingActions).isEqualTo(persistentListOf(withdraw, unstake))
            assertThat(confirmation(result).pendingAction).isEqualTo(withdraw)
        }

        @Test
        fun `GIVEN cosmos multiple actions WHEN transform THEN pending actions dropped`() {
            // Act
            val result = transform(
                pendingActions = persistentListOf(
                    MockStaking.pendingAction(StakingActionType.WITHDRAW),
                    MockStaking.pendingAction(StakingActionType.UNSTAKE),
                ),
            )

            // Assert
            assertThat(confirmation(result).pendingActions).isNull()
        }

        @Test
        fun `GIVEN explicit pending action WHEN transform THEN it overrides first of list`() {
            // Act
            val result = transform(
                pendingActions = persistentListOf(MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS)),
                pendingAction = MockStaking.pendingAction(StakingActionType.RESTAKE),
            )

            // Assert
            assertThat(observedAction(result)).isEqualTo(StakingActionCommonType.Pending.Restake)
            assertThat(confirmation(result).pendingAction?.type).isEqualTo(StakingActionType.RESTAKE)
        }
    }

    private fun partialDisabled(): StakingIntegration = stakeKitIntegration(
        id = StakingIntegrationID.StakeKit.Coin.Cosmos,
        yield = MockStaking.yield(
            id = MockStaking.COSMOS_ID,
            token = MockStaking.atomToken,
            enter = MockStaking.enterArgs(maximum = BigDecimal("-1")),
            exit = MockStaking.enterArgs(maximum = BigDecimal("-1")),
        ),
    )

    @Suppress("LongParameterList")
    private fun transform(
        isEnter: Boolean = false,
        isExplicitExit: Boolean = false,
        balanceState: BalanceState? = balanceState(),
        stakingApproval: StakingApproval = StakingApproval.Empty,
        stakingAllowance: BigDecimal = BigDecimal.ZERO,
        currency: CryptoCurrency = cosmos,
        integration: StakingIntegration = cosmosIntegration,
        pendingActions: ImmutableList<PendingAction>? = null,
        pendingAction: PendingAction? = pendingActions?.firstOrNull(),
        prevBlockchainId: String = currency.network.rawId,
    ): StakingUiState = SetConfirmationStateInitTransformer(
        isEnter = isEnter,
        isExplicitExit = isExplicitExit,
        balanceState = balanceState,
        stakingApproval = stakingApproval,
        stakingAllowance = stakingAllowance,
        cryptoCurrencyStatus = currencyStatus(currency),
        integration = integration,
        pendingActions = pendingActions,
        pendingAction = pendingAction,
    ).transform(stakingUiState(cryptoCurrencyBlockchainId = prevBlockchainId))

    private fun observedAction(result: StakingUiState) = result.actionType

    private fun observedEditable(result: StakingUiState) = confirmation(result).isAmountEditable

    private fun confirmation(result: StakingUiState) = result.confirmationState as StakingStates.ConfirmationState.Data

    internal data class MappingModel(val type: StakingActionType, val expected: StakingActionCommonType)
}