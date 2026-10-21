package com.tangem.features.staking.impl.presentation.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.PendingActionConstraints
import com.tangem.domain.models.staking.RewardBlockType
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.analytics.StakeScreenSource
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.transformers.SetAmountDataTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateInitTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ShowActionSelectorBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.validator.ValidatorSelectChangeTransformer
import com.tangem.utils.transformer.Transformer
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelValidatorTest : StakingModelTestBase() {

    @Test
    fun `GIVEN loaded model WHEN openValidators THEN analytics sent and step changed to Validators`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.openValidators()

        // Assert
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
    fun `GIVEN target WHEN onTargetSelect THEN analytics sent and ValidatorSelectChangeTransformer applied`() =
        runTest {
            // Arrange
            val target: StakingTarget = mockk(relaxed = true) {
                every { name } returns "TestValidator"
            }

            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onTargetSelect(target)

            // Assert
            verify {
                analyticsEventHandler.send(event = StakingAnalyticsEvent.ValidatorChosen("TestValidator"))
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ValidatorSelectChangeTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN no RewardsRequirementsError AND single reward WHEN openRewardsValidators THEN onActiveStake called`() =
        runTest {
            // Arrange
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

            // Act
            model.openRewardsValidators()
            advanceUntilIdle()

            // Assert
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
            // Arrange
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

            // Act
            model.openRewardsValidators()

            // Assert
            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            verify {
                analyticsEventHandler.send(
                    event = StakingAnalyticsEvent.ButtonValidator(source = StakeScreenSource.Info),
                )
            }
            verify {
                stateController.update(any<(StakingUiState) -> StakingUiState>())
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN several rewards WHEN openRewardsValidators THEN rewards action set and rewards step shown`() = runTest {
        // Arrange
        val currentState = stakingUiState()
        every { stateController.uiState } returns MutableStateFlow(currentState)
        val rewardsUiState = mockk<StakingUiState>(relaxed = true) {
            every { initialInfoState } returns initialInfoData(yieldBalance = yieldBalanceData())
            every { rewardsValidatorsState } returns mockk<StakingStates.RewardsValidatorsState.Data>(relaxed = true) {
                every { rewards } returns persistentListOf(balanceState(), balanceState(groupId = "group-2"))
            }
        }
        every { stateController.value } returns rewardsUiState
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val updates = mutableListOf<(StakingUiState) -> StakingUiState>()
        every { stateController.update(capture(updates)) } just Runs

        // Act
        model.openRewardsValidators()

        // Assert
        assertThat(updates.map { it(currentState) }).containsExactly(
            currentState.copy(actionType = StakingActionCommonType.Pending.Rewards),
            currentState.copy(currentStep = StakingStep.RewardsValidators),
        ).inOrder()

        model.onDestroy()
    }

    @Test
    fun `GIVEN RewardsRequirementsError AND minimumAmount WHEN openRewardsValidators THEN alert shown call`() =
        runTest {
            // Arrange
            every { messageSender.send(any()) } just Runs

            val constraints = PendingActionConstraints(
                type = StakingActionType.CLAIM_REWARDS,
                amountArg = PendingAction.PendingActionArgs.Amount(
                    isRequired = true,
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

            // Act
            model.openRewardsValidators()

            // Assert
            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            verify { messageSender.send(any()) }
            verify(exactly = 0) { getActionRequirementAmountUseCase.invoke(any(), any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN RewardsRequirementsError WHEN openRewardsValidators THEN getActionRequirementAmountUseCase called`() =
        runTest {
            // Arrange
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

            // Act
            model.openRewardsValidators()

            // Assert
            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            verify {
                getActionRequirementAmountUseCase.invoke(
                    integrationId = "test-integration",
                    actionType = StakingActionType.CLAIM_REWARDS,
                )
            }
            verify { messageSender.send(any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN single pending action WHEN onActiveStake THEN prepareForConfirmation and onNextClick called`() =
        runTest {
            // Arrange
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

            // Act
            model.onActiveStake(activeStake)
            advanceUntilIdle()

            // Assert — prepareForConfirmation calls updateAll with 4 transformers
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
            // Arrange
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

            // Act
            model.onActiveStake(activeStake)
            advanceUntilIdle()

            // Assert
            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is ShowActionSelectorBottomSheetTransformer },
                )
            }
            // prepareForConfirmation should NOT have been called
            verify(exactly = 0) {
                stateController.updateAll(
                    match { it is SetConfirmationStateInitTransformer },
                    any(),
                    any(),
                    any(),
                )
            }

            model.onDestroy()
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN rewards data missing WHEN openRewardsValidators THEN rewards validators screen shown`(
        model: MissingRewardsModel,
    ) = runTest {
        // Arrange
        every { stateController.value } returns model.state
        val stakingModel = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        stakingModel.openRewardsValidators()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(StakingAnalyticsEvent.ButtonValidator(source = StakeScreenSource.Info))
        }
        verify(exactly = 1) { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.RewardScreenOpened }) }

        stakingModel.onDestroy()
    }

    data class MissingRewardsModel(val state: StakingUiState)

    @ParameterizedTest
    @MethodSource("provideRewardsMinimumModels")
    fun `GIVEN rewards requirements error WHEN openRewardsValidators THEN alert shows minimum`(
        model: RewardsMinimumModel,
    ) = runTest {
        // Arrange
        val status = currencyStatus()
        val accountStatus = accountCurrencyStatusOf(status)
        every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns flowOf(accountStatus)
        val messages = mutableListOf<Any>()
        every { messageSender.send(any()) } answers { messages += firstArg<Any>() }
        val constraints = model.minimum?.let { minimum ->
            PendingActionConstraints(
                type = StakingActionType.CLAIM_REWARDS,
                amountArg = PendingAction.PendingActionArgs.Amount(isRequired = true, minimum = minimum, maximum = null),
            )
        }
        val yieldBalance = InnerYieldBalanceState.Data(
            integrationId = null,
            reward = YieldReward(
                rewardsCrypto = "1.0",
                rewardsFiat = "$1.00",
                rewardBlockType = RewardBlockType.RewardsRequirementsError,
                rewardConstraints = constraints,
            ),
            isActionable = true,
            balances = persistentListOf(),
        )
        every { stateController.value } returns stakingUiState(initialInfoState = initialInfoData(yieldBalance))
        val stakingModel = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        stakingModel.openRewardsValidators()

        // Assert
        val expectedAmount = model.minimum?.format { crypto(status.currency) }.orEmpty()
        assertThat(messages.filterIsInstance<DialogMessage>().map { it.message }).containsExactly(
            resourceReference(
                id = R.string.staking_details_min_rewards_notification,
                formatArgs = wrappedList(status.currency.name, expectedAmount),
            ),
        )

        stakingModel.onDestroy()
    }

    data class RewardsMinimumModel(val minimum: BigDecimal?)

    @Test
    fun `GIVEN P2PEthPool integration WHEN onActiveStake with single action THEN confirmation prepared`() = runTest {
        // Arrange
        coEvery { p2pEthPoolRepository.getVaultsSync() } returns emptyList()
        coEvery { p2pEthPoolRepository.getVaultLimitsSyncOrNull() } returns emptyMap()
        every { stateController.value } returns initialUiState
        val model = createModel(
            paramsContainer = MutableParamsContainer(
                StakingComponent.Params(
                    userWalletId = testUserWalletId,
                    cryptoCurrency = testCryptoCurrency,
                    integrationId = StakingIntegrationID.P2PEthPool,
                ),
            ),
            testScope = this,
        )
        advanceUntilIdle()

        // Act
        model.onActiveStake(balanceState(pendingActions = listOf(claimRewardsAction)))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            stateController.updateAll(match { it is SetConfirmationStateInitTransformer }, any(), any(), any())
        }

        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideExplicitExitModels")
    fun `GIVEN single non restake action WHEN onActiveStake THEN action type depends on balance type`(
        model: ExplicitExitModel,
    ) = runTest {
        // Arrange
        every { stateController.value } returns initialUiState
        val transformers = mutableListOf<Any?>()
        every { stateController.updateAll(*anyVararg()) } answers {
            args.forEach { arg -> if (arg is Array<*>) transformers.addAll(arg) else transformers.add(arg) }
        }
        val stakingModel = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        stakingModel.onActiveStake(balanceState(type = model.balanceType, pendingActions = model.pendingActions))
        advanceUntilIdle()

        // Assert
        val actual = transformers.filterIsInstance<SetConfirmationStateInitTransformer>().single()
            .transform(stakingUiState(cryptoCurrencyBlockchainId = "ethereum")).actionType
        assertThat(actual).isEqualTo(model.expected)

        stakingModel.onDestroy()
    }

    @Test
    fun `GIVEN single preferred validator WHEN onActiveStake with restake and claim THEN claim confirmed directly`() =
        runTest {
            // Arrange
            every { stateController.value } returns initialUiState
            every { testYield.validators } returns listOf(MockStaking.validator(preferred = true))
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val restake = PendingAction(type = StakingActionType.RESTAKE, passthrough = "restake", args = null)

            // Act
            model.onActiveStake(balanceState(pendingActions = listOf(restake, claimRewardsAction)))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                stateController.updateAll(match { it is SetConfirmationStateInitTransformer }, any(), any(), any())
            }
            verify(exactly = 0) {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is ShowActionSelectorBottomSheetTransformer },
                )
            }

            model.onDestroy()
        }

    data class ExplicitExitModel(
        val balanceType: BalanceType,
        val expected: StakingActionCommonType,
        val pendingActions: List<PendingAction> = listOf(claimRewardsAction),
    )

    companion object {
        private val claimRewardsAction = PendingAction(
            type = StakingActionType.CLAIM_REWARDS,
            passthrough = "claim",
            args = null,
        )

        @JvmStatic
        fun provideTestModels() = listOf(
            MissingRewardsModel(state = stakingUiState()),
            MissingRewardsModel(state = stakingUiState(initialInfoState = initialInfoData())),
        )

        @JvmStatic
        fun provideRewardsMinimumModels() = listOf(
            RewardsMinimumModel(minimum = BigDecimal("0.5")),
        )

        @JvmStatic
        fun provideExplicitExitModels() = listOf(
            ExplicitExitModel(
                balanceType = BalanceType.STAKED,
                expected = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false),
            ),
            ExplicitExitModel(balanceType = BalanceType.REWARDS, expected = StakingActionCommonType.Pending.Rewards),
            ExplicitExitModel(
                balanceType = BalanceType.STAKED,
                expected = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false),
                pendingActions = emptyList(),
            ),
        )
    }

    @Test
    fun `GIVEN loaded model WHEN onActiveStakeAnalytic THEN ButtonValidator analytics sent`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onActiveStakeAnalytic()

        // Assert
        verify {
            analyticsEventHandler.send(
                StakingAnalyticsEvent.ButtonValidator(
                    source = StakeScreenSource.Info,
                ),
            )
        }

        model.onDestroy()
    }
}