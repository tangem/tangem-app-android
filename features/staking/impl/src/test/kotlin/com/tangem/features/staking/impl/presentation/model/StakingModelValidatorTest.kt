package com.tangem.features.staking.impl.presentation.model

import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.PendingActionConstraints
import com.tangem.domain.models.staking.RewardBlockType
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.analytics.StakeScreenSource
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.transformers.SetAmountDataTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateInitTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ShowActionSelectorBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.validator.ValidatorSelectChangeTransformer
import com.tangem.utils.transformer.Transformer
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelValidatorTest : StakingModelTestBase() {

    @Test
    fun `WHEN openValidators THEN analytics sent and step changed to Validators`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.openValidators()

        verify {
            analyticsEventHandler.send(
                match { it is StakingAnalyticsEvent.ButtonValidator },
            )
        }
        verify {
            stateController.update(any<(StakingUiState) -> StakingUiState>())
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onTargetSelect THEN analytics sent and ValidatorSelectChangeTransformer applied`() = runTest {
        val target: StakingTarget = mockk(relaxed = true) {
            every { name } returns "TestValidator"
        }

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onTargetSelect(target)

        verify {
            analyticsEventHandler.send(event = StakingAnalyticsEvent.ValidatorChosen("TestValidator"))
        }
        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is ValidatorSelectChangeTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN no RewardsRequirementsError AND single reward WHEN openRewardsValidators THEN onActiveStake called`() =
        runTest {
            every { stateController.value } returns initialUiState
            every { messageSender.send(any()) } just Runs

            val singleReward: BalanceState = mockk(relaxed = true) {
                every { pendingActions } returns persistentListOf()
            }
            val yieldBalance = InnerYieldBalanceState.Data(
                integrationId = "test-integration",
                reward = YieldReward(
                    rewardsCrypto = "1.0",
                    rewardsFiat = "$1.00",
                    rewardBlockType = RewardBlockType.Rewards,
                    rewardConstraints = null,
                ),
                isActionable = true,
                balances = persistentListOf(),
            )
            val initialInfoState = mockk<StakingStates.InitialInfoState.Data>(relaxed = true) {
                every { this@mockk.yieldBalance } returns yieldBalance
            }
            val rewardsValidatorsState = mockk<StakingStates.RewardsValidatorsState.Data>(relaxed = true) {
                every { rewards } returns persistentListOf(singleReward)
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.initialInfoState } returns initialInfoState
                every { this@mockk.rewardsValidatorsState } returns rewardsValidatorsState
            }
            every { stateController.value } returns uiState

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.openRewardsValidators()
            advanceUntilIdle()

            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            // onActiveStake path — ButtonValidator analytics should NOT be sent
            verify(exactly = 0) {
                analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonValidator })
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN no RewardsRequirementsError AND rewards WHEN openRewardsValidators THEN showRewardsValidators called`() =
        runTest {
            every { stateController.value } returns initialUiState

            val reward1: BalanceState = mockk(relaxed = true)
            val reward2: BalanceState = mockk(relaxed = true)
            val yieldBalance = InnerYieldBalanceState.Data(
                integrationId = "test-integration",
                reward = YieldReward(
                    rewardsCrypto = "2.0",
                    rewardsFiat = "$2.00",
                    rewardBlockType = RewardBlockType.Rewards,
                    rewardConstraints = null,
                ),
                isActionable = true,
                balances = persistentListOf(),
            )
            val initialInfoState = mockk<StakingStates.InitialInfoState.Data>(relaxed = true) {
                every { this@mockk.yieldBalance } returns yieldBalance
            }
            val rewardsValidatorsState = mockk<StakingStates.RewardsValidatorsState.Data>(relaxed = true) {
                every { rewards } returns persistentListOf(reward1, reward2)
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.initialInfoState } returns initialInfoState
                every { this@mockk.rewardsValidatorsState } returns rewardsValidatorsState
            }
            every { stateController.value } returns uiState

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.openRewardsValidators()

            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            verify {
                analyticsEventHandler.send(
                    event = StakingAnalyticsEvent.ButtonValidator(source = StakeScreenSource.Info)
                )
            }
            verify {
                stateController.update(any<(StakingUiState) -> StakingUiState>())
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN RewardsRequirementsError AND minimumAmount WHEN openRewardsValidators THEN alert shown call`() =
        runTest {
            every { messageSender.send(any()) } just Runs

            val constraints = PendingActionConstraints(
                type = StakingActionType.CLAIM_REWARDS,
                amountArg = PendingAction.PendingActionArgs.Amount(
                    required = true,
                    minimum = BigDecimal.TEN,
                    maximum = null,
                ),
            )
            val yieldBalance = InnerYieldBalanceState.Data(
                integrationId = "test-integration",
                reward = YieldReward(
                    rewardsCrypto = "1.0",
                    rewardsFiat = "$1.00",
                    rewardBlockType = RewardBlockType.RewardsRequirementsError,
                    rewardConstraints = constraints,
                ),
                isActionable = true,
                balances = persistentListOf(),
            )
            val initialInfoState = mockk<StakingStates.InitialInfoState.Data>(relaxed = true) {
                every { this@mockk.yieldBalance } returns yieldBalance
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.initialInfoState } returns initialInfoState
            }
            every { stateController.value } returns uiState

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.openRewardsValidators()

            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            verify { messageSender.send(any()) }
            verify(exactly = 0) { getActionRequirementAmountUseCase.invoke(any(), any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN RewardsRequirementsError WHEN openRewardsValidators THEN getActionRequirementAmountUseCase called`() =
        runTest {
            every { messageSender.send(any()) } just Runs
            every {
                getActionRequirementAmountUseCase.invoke(any(), any())
            } returns BigDecimal.ONE

            val yieldBalance = InnerYieldBalanceState.Data(
                integrationId = "test-integration",
                reward = YieldReward(
                    rewardsCrypto = "1.0",
                    rewardsFiat = "$1.00",
                    rewardBlockType = RewardBlockType.RewardsRequirementsError,
                    rewardConstraints = null,
                ),
                isActionable = true,
                balances = persistentListOf(),
            )
            val initialInfoState = mockk<StakingStates.InitialInfoState.Data>(relaxed = true) {
                every { this@mockk.yieldBalance } returns yieldBalance
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.initialInfoState } returns initialInfoState
            }
            every { stateController.value } returns uiState

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.openRewardsValidators()

            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            verify {
                getActionRequirementAmountUseCase.invoke(
                    integrationId = "test-integration",
                    actionType = StakingActionType.CLAIM_REWARDS
                )
            }
            verify { messageSender.send(any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN single pending action WHEN onActiveStake THEN prepareForConfirmation and onNextClick called`() =
        runTest {
            every { stateController.value } returns initialUiState

            val singleAction = PendingAction(
                type = StakingActionType.CLAIM_REWARDS,
                passthrough = "test",
                args = null,
            )
            val activeStake: BalanceState = mockk(relaxed = true) {
                every { type } returns BalanceType.STAKED
                every { pendingActions } returns persistentListOf(singleAction)
                every { target } returns null
                every { cryptoValue } returns "100"
            }

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onActiveStake(activeStake)
            advanceUntilIdle()

            // prepareForConfirmation calls updateAll with 4 transformers
            verify {
                stateController.updateAll(
                    match { it is SetConfirmationStateInitTransformer },
                    match { it is ValidatorSelectChangeTransformer },
                    match { it is SetAmountDataTransformer },
                    any(),
                )
            }
            // onNextClick updates step
            verify { stateController.update(any<(StakingUiState) -> StakingUiState>()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN multiple pending actions WHEN onActiveStake THEN ShowActionSelectorBottomSheetTransformer applied`() =
        runTest {
            every { stateController.value } returns initialUiState

            val action1 = PendingAction(
                type = StakingActionType.CLAIM_REWARDS,
                passthrough = "test1",
                args = null,
            )
            val action2 = PendingAction(
                type = StakingActionType.WITHDRAW,
                passthrough = "test2",
                args = null,
            )
            val activeStake: BalanceState = mockk(relaxed = true) {
                every { type } returns BalanceType.STAKED
                every { pendingActions } returns persistentListOf(action1, action2)
                every { target } returns null
                every { cryptoValue } returns "100"
            }

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onActiveStake(activeStake)

            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is ShowActionSelectorBottomSheetTransformer },
                )
            }
            // prepareForConfirmation should NOT have been called
            verify(exactly = 0) {
                stateController.updateAll(
                    match { it is SetConfirmationStateInitTransformer },
                    any(), any(), any(),
                )
            }

            model.onDestroy()
        }

    @Test
    fun `WHEN onActiveStakeAnalytic THEN ButtonValidator analytics sent`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onActiveStakeAnalytic()

        verify {
            analyticsEventHandler.send(
                StakingAnalyticsEvent.ButtonValidator(
                    source = StakeScreenSource.Info,
                )
            )
        }

        model.onDestroy()
    }
}