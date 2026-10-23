package com.tangem.features.staking.impl.analytics.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.analytics.StakeScreenSource
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.polygonToken
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.balanceState
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.initialInfoData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.features.staking.impl.presentation.state.target
import com.tangem.features.staking.impl.presentation.state.validatorData
import com.tangem.features.staking.impl.presentation.state.yieldBalanceData
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class StakingAnalyticSenderTest {

    private val handler: AnalyticsEventHandler = mockk()
    private val events = mutableListOf<AnalyticsEvent>()
    private val sender = StakingAnalyticSender(handler)

    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)
    private val exit = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)

    @BeforeEach
    fun setUp() {
        clearMocks(handler)
        events.clear()
        every { handler.send(capture(events)) } just Runs
    }

    @Nested
    inner class InitialInfoScreen {

        @Test
        fun `GIVEN no initial data WHEN initialInfoScreen THEN zero validators`() {
            // Act
            sender.initialInfoScreen(stakingUiState())

            // Assert
            assertThat(events).containsExactly(StakingAnalyticsEvent.StakingInfoScreenOpened(validatorsCount = 0))
        }

        @Test
        fun `GIVEN balances with repeated and missing targets WHEN initialInfoScreen THEN distinct non blank count`() {
            // Arrange
            val first = target()
            val second = target(MockStaking.validator(MockStaking.SECOND_VALIDATOR_ADDRESS, name = "Validator 2"))
            val blank = target(MockStaking.validator(address = " "))
            val state = stakingUiState(
                initialInfoState = initialInfoData(
                    yieldBalance = yieldBalanceData(
                        listOf(
                            balanceState(target = first),
                            balanceState(target = first, groupId = "2"),
                            balanceState(target = second),
                            balanceState(target = null),
                            balanceState(target = blank),
                        ),
                    ),
                ),
            )

            // Act
            sender.initialInfoScreen(state)

            // Assert
            assertThat(events).containsExactly(StakingAnalyticsEvent.StakingInfoScreenOpened(validatorsCount = 2))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConfirmationScreen {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN chosen validator and action WHEN confirmationScreen THEN event with mapped action`(
            model: ActionModel,
        ) {
            // Arrange
            val state = stakingUiState(
                actionType = model.actionType,
                validatorState = validatorData(),
                confirmationState = model.confirmationState,
            )

            // Act
            sender.confirmationScreen(state)

            // Assert
            assertThat(events).containsExactly(
                StakingAnalyticsEvent.ConfirmationScreenOpened(validator = "Validator 1", action = model.expected),
            )
        }

        @Test
        fun `GIVEN no validator WHEN confirmationScreen THEN nothing sent`() {
            // Act
            sender.confirmationScreen(stakingUiState(confirmationState = confirmationData()))

            // Assert
            assertThat(events).isEmpty()
        }

        @Test
        fun `GIVEN completed WHEN confirmationScreen THEN nothing sent`() {
            // Arrange
            val state = stakingUiState(
                validatorState = validatorData(),
                confirmationState = confirmationData(innerState = InnerConfirmationStakingState.COMPLETED),
            )

            // Act
            sender.confirmationScreen(state)

            // Assert
            assertThat(events).isEmpty()
        }

        private fun provideTestModels() = listOf(
            ActionModel(enter, StakingActionType.STAKE),
            ActionModel(exit, StakingActionType.UNSTAKE),
            ActionModel(
                actionType = StakingActionCommonType.Pending.Rewards,
                expected = StakingActionType.CLAIM_REWARDS,
                confirmationState = confirmationData(
                    pendingAction = MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS),
                ),
            ),
            ActionModel(StakingActionCommonType.Pending.Other, StakingActionType.UNKNOWN),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ScreenCancel {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN step WHEN screenCancel THEN cancel with mapped source`(model: CancelModel) {
            // Act
            sender.screenCancel(stakingUiState(currentStep = model.step))

            // Assert
            assertThat(events).containsExactly(StakingAnalyticsEvent.ButtonCancel(model.source))
        }

        private fun provideTestModels() = listOf(
            CancelModel(StakingStep.InitialInfo, StakeScreenSource.Info),
            CancelModel(StakingStep.Amount, StakeScreenSource.Amount),
            CancelModel(StakingStep.Success, StakeScreenSource.Confirmation),
            CancelModel(StakingStep.Confirmation, StakeScreenSource.Confirmation),
            CancelModel(StakingStep.Validators, StakeScreenSource.Validators),
            CancelModel(StakingStep.RestakeValidator, StakeScreenSource.Validators),
            CancelModel(StakingStep.RewardsValidators, StakeScreenSource.Validators),
        )
    }

    @Nested
    inner class Transactions {

        @Test
        fun `GIVEN token WHEN sendTransactionApprovalAnalytics THEN approve transaction sent params`() {
            // Arrange
            val token = polygonToken()

            // Act
            sender.sendTransactionApprovalAnalytics(token)

            // Assert
            val event = events.single()
            assertThat(event).isInstanceOf(Basic.TransactionSent::class.java)
            assertThat(event.params).containsAtLeastEntriesIn(
                mapOf(
                    AnalyticsParam.SOURCE to "Approve",
                    AnalyticsParam.BLOCKCHAIN to token.network.name,
                    AnalyticsParam.TOKEN_PARAM to token.symbol,
                    AnalyticsParam.FEE_TOKEN to token.symbol,
                    AnalyticsParam.FEE_TYPE to AnalyticsParam.FeeType.Normal.value,
                    AnalyticsParam.FEE_ASSET_TYPE to "Coin",
                    AnalyticsParam.PERMISSION_TYPE to ApproveType.LIMITED.name,
                ),
            )
        }

        @Test
        fun `GIVEN chosen validator WHEN sendTransactionStakingAnalytics THEN transaction sent then in progress event`() {
            // Arrange
            val status = currencyStatus()
            val state = stakingUiState(
                cryptoCurrencySymbol = "UI-SYM",
                actionType = exit,
                validatorState = validatorData(),
            )

            // Act
            sender.sendTransactionStakingAnalytics(state, status)

            // Assert
            assertThat(events).hasSize(2)
            assertThat(events[0]).isInstanceOf(Basic.TransactionSent::class.java)
            assertThat(events[0].params).containsAtLeastEntriesIn(
                mapOf(
                    AnalyticsParam.SOURCE to "Staking",
                    AnalyticsParam.TOKEN_PARAM to "UI-SYM",
                    AnalyticsParam.FEE_TOKEN to status.currency.symbol,
                    AnalyticsParam.BLOCKCHAIN to status.currency.network.name,
                ),
            )
            assertThat(events[1]).isEqualTo(
                StakingAnalyticsEvent.StakeInProgressScreenOpened(
                    validator = "Validator 1",
                    action = StakingActionType.UNSTAKE,
                    feeAssetType = AnalyticsParam.FeeAssetType.Coin,
                ),
            )
        }

        @Test
        fun `GIVEN no validator WHEN sendTransactionStakingAnalytics THEN nothing sent`() {
            // Act
            sender.sendTransactionStakingAnalytics(stakingUiState(), currencyStatus())

            // Assert
            assertThat(events).isEmpty()
        }

        @Test
        fun `GIVEN chosen validator WHEN sendTransactionStakingClickedAnalytics THEN button action event`() {
            // Act
            sender.sendTransactionStakingClickedAnalytics(
                stakingUiState(actionType = enter, validatorState = validatorData()),
            )

            // Assert
            assertThat(events).containsExactly(
                StakingAnalyticsEvent.ButtonAction(action = StakingActionType.STAKE, validator = "Validator 1"),
            )
        }

        @Test
        fun `GIVEN no validator WHEN sendTransactionStakingClickedAnalytics THEN nothing sent`() {
            // Act
            sender.sendTransactionStakingClickedAnalytics(stakingUiState())

            // Assert
            assertThat(events).isEmpty()
        }
    }

    internal data class ActionModel(
        val actionType: StakingActionCommonType,
        val expected: StakingActionType,
        val confirmationState: StakingStates.ConfirmationState = StakingStates.ConfirmationState.Empty(),
    )

    internal data class CancelModel(val step: StakingStep, val source: StakeScreenSource)
}