package com.tangem.features.staking.impl.presentation.state

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.staking.model.action.StakingActionType
import com.tangem.domain.staking.analytics.StakeScreenSource
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class StakingStateRouterTest {

    private val navigationRouter: Router = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val events = mutableListOf<AnalyticsEvent>()

    private lateinit var controller: StakingStateController
    private lateinit var router: StakingStateRouter

    @BeforeEach
    fun setUp() {
        clearMocks(navigationRouter, analytics)
        events.clear()
        every { analytics.send(capture(events)) } just Runs
        controller = StakingStateController(urlOpener = mockk(relaxed = true))
        router = StakingStateRouter(
            router = navigationRouter,
            stateController = controller,
            analyticsEventsHandler = analytics,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnNextClick {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN step and action type WHEN onNextClick THEN next step`(model: StepModel) {
            // Arrange
            givenState(model.step, model.actionType)

            // Act
            router.onNextClick()

            // Assert
            assertThat(controller.value.currentStep).isEqualTo(model.expectedStep)
        }

        @Test
        fun `GIVEN initial info enter WHEN onNextClick THEN amount screen opened event`() {
            // Arrange
            givenState(StakingStep.InitialInfo, StakingActionCommonType.Enter(shouldSkipEnterAmount = false))

            // Act
            router.onNextClick()

            // Assert
            assertThat(events.single()).isInstanceOf(StakingAnalyticsEvent.AmountScreenOpened::class.java)
        }

        @Test
        fun `GIVEN amount step with chosen validator WHEN onNextClick THEN confirmation screen opened with stake action`() {
            // Arrange
            givenState(StakingStep.Amount, StakingActionCommonType.Enter(shouldSkipEnterAmount = false))

            // Act
            router.onNextClick()

            // Assert
            assertThat(events).containsExactly(confirmationOpened(StakingActionType.STAKE))
        }

        @Test
        fun `GIVEN exit to confirmation WHEN onNextClick THEN confirmation event with unstake action`() {
            // Arrange
            givenState(StakingStep.InitialInfo, StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = true))

            // Act
            router.onNextClick()

            // Assert
            assertThat(events).containsExactly(confirmationOpened(StakingActionType.UNSTAKE))
        }

        @Test
        fun `GIVEN pending claim WHEN onNextClick THEN confirmation event with pending action type`() {
            // Arrange
            givenState(
                step = StakingStep.InitialInfo,
                actionType = StakingActionCommonType.Pending.Rewards,
                confirmationState = confirmationData(
                    pendingAction = MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS),
                ),
            )

            // Act
            router.onNextClick()

            // Assert
            assertThat(events).containsExactly(confirmationOpened(StakingActionType.CLAIM_REWARDS))
        }

        @Test
        fun `GIVEN pending without confirmation data WHEN onNextClick THEN confirmation event with unknown action`() {
            // Arrange
            givenState(StakingStep.InitialInfo, StakingActionCommonType.Pending.Other)

            // Act
            router.onNextClick()

            // Assert
            assertThat(events).containsExactly(confirmationOpened(StakingActionType.UNKNOWN))
        }

        @Test
        fun `GIVEN no validator state WHEN onNextClick to confirmation THEN no confirmation event`() {
            // Arrange
            givenState(
                step = StakingStep.Amount,
                actionType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
                validatorState = StakingStates.ValidatorState.Empty(),
            )

            // Act
            router.onNextClick()

            // Assert
            assertThat(events).isEmpty()
            assertThat(controller.value.currentStep).isEqualTo(StakingStep.Confirmation)
        }

        @Test
        fun `GIVEN completed confirmation WHEN onNextClick to confirmation THEN no confirmation event`() {
            // Arrange
            givenState(
                step = StakingStep.Amount,
                actionType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
                confirmationState = confirmationData(innerState = InnerConfirmationStakingState.COMPLETED),
            )

            // Act
            router.onNextClick()

            // Assert
            assertThat(events).isEmpty()
        }

        @Test
        fun `GIVEN restake WHEN onNextClick THEN no analytics`() {
            // Arrange
            givenState(StakingStep.InitialInfo, StakingActionCommonType.Pending.Restake)

            // Act
            router.onNextClick()

            // Assert
            assertThat(events).isEmpty()
        }

        @Test
        fun `GIVEN success with two staked validators WHEN onNextClick THEN info screen opened with validators count`() {
            // Arrange
            val first = target()
            val second = target(MockStaking.validator(MockStaking.SECOND_VALIDATOR_ADDRESS, name = "Validator 2"))
            givenState(
                step = StakingStep.Success,
                actionType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
                initialInfoState = initialInfoData(
                    yieldBalance = yieldBalanceData(
                        listOf(
                            balanceState(target = first),
                            balanceState(target = first, groupId = "2"),
                            balanceState(target = second),
                            balanceState(target = null),
                        ),
                    ),
                ),
            )

            // Act
            router.onNextClick()

            // Assert
            assertThat(events).containsExactly(StakingAnalyticsEvent.StakingInfoScreenOpened(validatorsCount = 2))
        }

        @Test
        fun `GIVEN confirmation without initial data WHEN onNextClick THEN info event with zero validators`() {
            // Arrange
            givenState(StakingStep.Confirmation, StakingActionCommonType.Enter(shouldSkipEnterAmount = false))

            // Act
            router.onNextClick()

            // Assert
            assertThat(events).containsExactly(StakingAnalyticsEvent.StakingInfoScreenOpened(validatorsCount = 0))
        }

        private fun provideTestModels() = listOf(
            StepModel(StakingStep.InitialInfo, enter(), StakingStep.Amount),
            StepModel(StakingStep.InitialInfo, enter(skip = true), StakingStep.Confirmation),
            StepModel(StakingStep.InitialInfo, exit(), StakingStep.Amount),
            StepModel(StakingStep.InitialInfo, exit(disabled = true), StakingStep.Confirmation),
            StepModel(StakingStep.InitialInfo, StakingActionCommonType.Pending.Other, StakingStep.Confirmation),
            StepModel(StakingStep.InitialInfo, StakingActionCommonType.Pending.Rewards, StakingStep.Confirmation),
            StepModel(
                StakingStep.InitialInfo,
                StakingActionCommonType.Pending.Stake(shouldSkipEnterAmount = false),
                StakingStep.RestakeValidator,
            ),
            StepModel(
                StakingStep.InitialInfo,
                StakingActionCommonType.Pending.Stake(shouldSkipEnterAmount = true),
                StakingStep.RestakeValidator,
            ),
            StepModel(StakingStep.InitialInfo, StakingActionCommonType.Pending.Restake, StakingStep.RestakeValidator),
            StepModel(
                StakingStep.RestakeValidator,
                StakingActionCommonType.Pending.Restake,
                StakingStep.Confirmation,
            ),
            StepModel(
                StakingStep.RewardsValidators,
                StakingActionCommonType.Pending.Rewards,
                StakingStep.Confirmation,
            ),
            StepModel(StakingStep.Validators, enter(), StakingStep.Confirmation),
            StepModel(StakingStep.Amount, enter(), StakingStep.Confirmation),
            StepModel(StakingStep.Confirmation, enter(), StakingStep.InitialInfo),
            StepModel(StakingStep.Success, enter(), StakingStep.InitialInfo),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnPrevClick {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN step and action type WHEN onPrevClick THEN previous step`(model: StepModel) {
            // Arrange
            givenState(model.step, model.actionType)

            // Act
            router.onPrevClick()

            // Assert
            assertThat(controller.value.currentStep).isEqualTo(model.expectedStep)
        }

        @Test
        fun `GIVEN initial info WHEN onPrevClick THEN cancel event pop and state cleared`() {
            // Arrange
            givenState(StakingStep.InitialInfo, enter())

            // Act
            router.onPrevClick()

            // Assert
            assertThat(events).contains(StakingAnalyticsEvent.ButtonCancel(StakeScreenSource.Info))
            verify(exactly = 1) { navigationRouter.pop(any()) }
            assertThat(controller.value.cryptoCurrencyName).isEmpty()
        }

        @Test
        fun `GIVEN success WHEN onPrevClick THEN popped without clear and step kept`() {
            // Arrange
            givenState(StakingStep.Success, enter())

            // Act
            router.onPrevClick()

            // Assert
            verify(exactly = 1) { navigationRouter.pop(any()) }
            assertThat(controller.value.currentStep).isEqualTo(StakingStep.Success)
            assertThat(controller.value.cryptoCurrencyName).isEqualTo("Solana")
            assertThat(events).isEmpty()
        }

        @Test
        fun `GIVEN amount WHEN onPrevClick THEN info screen event sent`() {
            // Arrange
            givenState(StakingStep.Amount, enter())

            // Act
            router.onPrevClick()

            // Assert
            assertThat(events).containsExactly(StakingAnalyticsEvent.StakingInfoScreenOpened(validatorsCount = 0))
        }

        private fun provideTestModels() = listOf(
            StepModel(StakingStep.RestakeValidator, StakingActionCommonType.Pending.Restake, StakingStep.InitialInfo),
            StepModel(StakingStep.RewardsValidators, StakingActionCommonType.Pending.Rewards, StakingStep.InitialInfo),
            StepModel(StakingStep.Amount, enter(), StakingStep.InitialInfo),
            StepModel(StakingStep.Confirmation, enter(), StakingStep.Amount),
            StepModel(StakingStep.Confirmation, enter(skip = true), StakingStep.InitialInfo),
            StepModel(StakingStep.Confirmation, exit(), StakingStep.Amount),
            StepModel(StakingStep.Confirmation, exit(disabled = true), StakingStep.InitialInfo),
            StepModel(StakingStep.Confirmation, StakingActionCommonType.Pending.Rewards, StakingStep.InitialInfo),
            StepModel(
                StakingStep.Confirmation,
                StakingActionCommonType.Pending.Stake(shouldSkipEnterAmount = false),
                StakingStep.InitialInfo,
            ),
            StepModel(StakingStep.Validators, enter(), StakingStep.Confirmation),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Other {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN any step WHEN onBackClick THEN cancel event with step source`(model: CancelModel) {
            // Arrange
            givenState(model.step, enter())

            // Act
            router.onBackClick()

            // Assert
            assertThat(events.first()).isEqualTo(StakingAnalyticsEvent.ButtonCancel(model.source))
            verify(exactly = 1) { navigationRouter.pop(any()) }
            assertThat(controller.value.cryptoCurrencyName).isEmpty()
            assertThat(controller.value.currentStep).isEqualTo(StakingStep.InitialInfo)
        }

        @Test
        fun `GIVEN initial info WHEN showRewardsValidators THEN reward screen event and rewards step`() {
            // Arrange
            givenState(StakingStep.InitialInfo, enter())

            // Act
            router.showRewardsValidators()

            // Assert
            assertThat(controller.value.currentStep).isEqualTo(StakingStep.RewardsValidators)
            assertThat(events.single()).isInstanceOf(StakingAnalyticsEvent.RewardScreenOpened::class.java)
        }

        private fun provideTestModels() = listOf(
            CancelModel(StakingStep.InitialInfo, StakeScreenSource.Info),
            CancelModel(StakingStep.Amount, StakeScreenSource.Amount),
            CancelModel(StakingStep.Confirmation, StakeScreenSource.Confirmation),
            CancelModel(StakingStep.Success, StakeScreenSource.Confirmation),
            CancelModel(StakingStep.Validators, StakeScreenSource.Validators),
            CancelModel(StakingStep.RestakeValidator, StakeScreenSource.Validators),
            CancelModel(StakingStep.RewardsValidators, StakeScreenSource.Validators),
        )
    }

    private fun givenState(
        step: StakingStep,
        actionType: StakingActionCommonType,
        validatorState: StakingStates.ValidatorState = validatorData(),
        confirmationState: StakingStates.ConfirmationState = StakingStates.ConfirmationState.Empty(),
        initialInfoState: StakingStates.InitialInfoState = StakingStates.InitialInfoState.Empty(),
    ) {
        controller.update {
            stakingUiState(
                currentStep = step,
                actionType = actionType,
                validatorState = validatorState,
                confirmationState = confirmationState,
                initialInfoState = initialInfoState,
            )
        }
    }

    private fun confirmationOpened(action: StakingActionType) =
        StakingAnalyticsEvent.ConfirmationScreenOpened(validator = "Validator 1", action = action)

    private fun enter(skip: Boolean = false) = StakingActionCommonType.Enter(shouldSkipEnterAmount = skip)

    private fun exit(disabled: Boolean = false) = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = disabled)

    internal data class StepModel(
        val step: StakingStep,
        val actionType: StakingActionCommonType,
        val expectedStep: StakingStep,
    )

    internal data class CancelModel(val step: StakingStep, val source: StakeScreenSource)
}