package com.tangem.features.staking.impl.presentation.model

import arrow.core.Either
import com.tangem.core.analytics.models.Basic
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.state.transformers.*
import com.tangem.features.staking.impl.presentation.state.transformers.amount.AmountMaxValueStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.validator.ValidatorSelectChangeTransformer
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.transformer.Transformer
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelNavigationTest : StakingModelTestBase() {

    @Test
    fun `WHEN onBackClick THEN router pop and stateController clear called`() = runTest {
        every { stateController.value } returns initialUiState
        every { appRouter.pop(any()) } just Runs
        every { stateController.clear() } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onBackClick()

        verify { appRouter.pop(any()) }
        verify { stateController.clear() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN targets AND no yield balance WHEN onNextClick with balance THEN validators unavailable alert sent`() =
        runTest {
            every { messageSender.send(any()) } just Runs
            every { testYield.allValidatorsFull } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onNextClick(balanceState = null)
            advanceUntilIdle()

            verify {
                messageSender.send(
                    match { it is DialogMessage } // dialog from StakingModel.stakingEventFactory
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN partial amount disabled WHEN onNextClick with null balance THEN updateAll called with transformers`() =
        runTest {
            every { stateController.value } returns initialUiState
            every { testYield.args.enter.isPartialAmountDisabled } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onNextClick(balanceState = null)
            advanceUntilIdle()

            verify {
                stateController.updateAll(
                    match { it is SetConfirmationStateInitTransformer },
                    match { it is ValidatorSelectChangeTransformer },
                    match { it is SetAmountDataTransformer },
                    match { it is AmountMaxValueStateTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN non-initial step WHEN onNextClick THEN only stakingStateRouter onNextClick called`() = runTest {
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        every { stateController.value } returns initialUiState

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val amountUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Amount
        }
        uiStateFlow.value = amountUiState
        every { stateController.value } returns amountUiState
        advanceUntilIdle()

        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)

        model.onNextClick(balanceState = null)
        advanceUntilIdle()

        verify {
            stateController.update(match<(StakingUiState) -> StakingUiState> { true })
        }
        verify(exactly = 0) {
            stateController.updateAll(*anyVararg())
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN assent state AND no approval in progress WHEN onPrevClick THEN prev navigated and assent reset`() =
        runTest {
            val uiStateFlow = MutableStateFlow(initialUiState)
            every { stateController.uiState } returns uiStateFlow
            every {
                stakingOperationsFactory.createFeeLoader(
                    cryptoCurrencyStatus = any(),
                    userWallet = any(),
                    integration = any()
                )
            } returns mockk {
                coEvery {
                    getFee(any(), any(), any(), any())
                } just Runs
            }

            val model = createModel(testScope = this)
            advanceUntilIdle()

            val assentUiState = mockk<StakingUiState>(relaxed = true) {
                every { currentStep } returns StakingStep.Confirmation
                every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                    every { innerState } returns InnerConfirmationStakingState.ASSENT
                    every { notifications } returns persistentListOf()
                }
            }
            uiStateFlow.value = assentUiState
            every { stateController.value } returns assentUiState
            advanceUntilIdle()

            clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)
            every { stateController.update(any<Transformer<StakingUiState>>()) } just Runs
            every { stateController.update(any<(StakingUiState) -> StakingUiState>()) } just Runs
            every { stateController.value } returns assentUiState

            model.onPrevClick()

            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> {
                        it is SetConfirmationStateResetAssentTransformer
                    },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN in progress state WHEN onPrevClick THEN nothing happens`() = runTest {
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockk {
            coEvery {
                getFee(any(), any(), any(), any())
            } just Runs
        }

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val inProgressUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns InnerConfirmationStakingState.IN_PROGRESS
            }
        }
        uiStateFlow.value = inProgressUiState
        every { stateController.value } returns inProgressUiState
        advanceUntilIdle()

        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)

        model.onPrevClick()

        verify(exactly = 0) { stateController.update(any<Transformer<StakingUiState>>()) }
        verify(exactly = 0) { stateController.update(any<(StakingUiState) -> StakingUiState>()) }
        verify(exactly = 0) { stateController.updateAll(*anyVararg()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN completed state WHEN onPrevClick THEN onNextClick called`() = runTest {
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockk {
            coEvery {
                getFee(any(), any(), any(), any())
            } just Runs
        }

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val completedUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns InnerConfirmationStakingState.COMPLETED
            }
        }
        uiStateFlow.value = completedUiState
        every { stateController.value } returns completedUiState
        advanceUntilIdle()

        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)
        every { stateController.value } returns completedUiState
        every { stateController.update(any<(StakingUiState) -> StakingUiState>()) } just Runs
        every { stateController.clear() } just Runs

        model.onPrevClick()
        advanceUntilIdle()

        verify {
            stateController.update(any<(StakingUiState) -> StakingUiState>())
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN non-confirmation step WHEN onPrevClick THEN stakingStateRouter onPrevClick called`() = runTest {
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        every { stateController.value } returns initialUiState
        every { stateController.update(any<(StakingUiState) -> StakingUiState>()) } just Runs
        every { appRouter.pop(any()) } just Runs
        every { stateController.clear() } just Runs

        val amountUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Amount
        }

        val model = createModel(testScope = this)
        advanceUntilIdle()

        uiStateFlow.value = amountUiState
        every { stateController.value } returns amountUiState
        every { stateController.uiState } returns MutableStateFlow(amountUiState)

        model.onPrevClick()

        verify { stateController.update(any<(StakingUiState) -> StakingUiState>()) }

        model.onDestroy()
    }

    @Test
    fun `WHEN onRefreshSwipe true THEN loading set and balanceUpdater partialUpdate called`() = runTest {
        val testAppScope = object : AppCoroutineScope,
            CoroutineScope by this {}

        val model = createModel(
            testScope = this,
            coroutineScope = testAppScope,
        )
        advanceUntilIdle()

        model.onRefreshSwipe(isRefreshing = true)
        advanceUntilIdle()

        verify {
            stateController.update(
                match<Transformer<StakingUiState>> {
                    it is SetInitialLoadingStateTransformer
                }
            )
        }
        coVerify { mockBalanceUpdater.partialUpdate() }

        model.onDestroy()
    }

    @Test
    fun `WHEN onInitialInfoBannerClick THEN analytics sent and url opened`() = runTest {
        every { innerRouter.openUrl(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onInitialInfoBannerClick()

        verify {
            analyticsEventHandler.send(match { it is StakingAnalyticsEvent.WhatIsStaking })
        }
        verify {
            innerRouter.openUrl("https://tangem.com/en/blog/post/how-to-stake-cryptocurrency/")
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onInfoClick THEN ShowInfoBottomSheetStateTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onInfoClick(InfoType.ANNUAL_PERCENTAGE_RATE)

        verify {
            stateController.update(
                match<Transformer<StakingUiState>> {
                    it is ShowInfoBottomSheetStateTransformer
                }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN empty preferredTargets WHEN onAmountEnterClick THEN noAvailableValidators alert sent`() = runTest {
        every { testYield.preferredValidators } returns emptyList()
        every { messageSender.send(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAmountEnterClick()

        verify { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN non-empty preferredTargets WHEN onAmountEnterClick THEN validator reset and onNextClick called`() =
        runTest {
            every { stateController.value } returns initialUiState
            every { testYield.preferredValidators } returns listOf(mockk(relaxed = true))
            every { initialUiState.actionType } returns StakingActionCommonType.Enter(skipEnterAmount = false)
            every { testYield.args.enter.isPartialAmountDisabled } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onAmountEnterClick()
            advanceUntilIdle()

            verify {
                stateController.updateAll(
                    match { it is ValidatorSelectChangeTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN txUrl not null WHEN onExploreClick THEN analytics sent and url opened`() = runTest {
        val txUrl = "https://explorer.solana.com/tx/abc123"
        val transactionDoneState = TransactionDoneState.Content(
            timestamp = 1000L,
            txUrl = txUrl,
        )
        val confirmationState = mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
            every { this@mockk.transactionDoneState } returns transactionDoneState
        }
        val uiState = mockk<StakingUiState>(relaxed = true) {
            every { this@mockk.confirmationState } returns confirmationState
        }
        every { stateController.uiState } returns MutableStateFlow(uiState)
        every { innerRouter.openUrl(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onExploreClick()

        verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonExplore }) }
        verify { innerRouter.openUrl(txUrl) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN txUrl not null WHEN onShareClick THEN analytics sent and shareManager called`() = runTest {
        val txUrl = "https://explorer.solana.com/tx/abc123"
        val transactionDoneState = TransactionDoneState.Content(
            timestamp = 1000L,
            txUrl = txUrl,
        )
        val confirmationState = mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
            every { this@mockk.transactionDoneState } returns transactionDoneState
        }
        val uiState = mockk<StakingUiState>(relaxed = true) {
            every { this@mockk.confirmationState } returns confirmationState
        }
        every { stateController.uiState } returns MutableStateFlow(uiState)
        every { vibratorHapticManager.performOneTime(any()) } just Runs
        every { shareManager.shareText(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onShareClick()

        verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonShare }) }
        verify { vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click) }
        verify { shareManager.shareText(txUrl) }

        model.onDestroy()
    }

    @Test
    fun `WHEN onFailedTxEmailClick THEN analytics sent and sendFeedbackEmail called`() = runTest {
        coEvery { getWalletMetaInfoUseCase(userWalletId = any()) } returns Either.Right(mockk(relaxed = true))
        every { saveBlockchainErrorUseCase(error = any()) } just Runs
        coEvery { sendFeedbackEmailUseCase(type = any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onFailedTxEmailClick("test error")
        advanceUntilIdle()

        verify { analyticsEventHandler.send(match { it is Basic.ButtonSupport }) }
        coVerify { sendFeedbackEmailUseCase(match { it is FeedbackEmailType.StakingProblem }) }

        model.onDestroy()
    }

    @Test
    fun `WHEN openTokenDetails THEN innerRouter openTokenDetails called`() = runTest {
        every { innerRouter.openTokenDetails(any(), any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val currency: CryptoCurrency = mockk(relaxed = true)
        model.openTokenDetails(currency)

        verify { innerRouter.openTokenDetails(testUserWalletId, currency) }

        model.onDestroy()
    }

    @Test
    fun `WHEN showPrimaryClickAlert THEN messageSender sends alert`() = runTest {
        every { messageSender.send(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.showPrimaryClickAlert()

        verify { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `WHEN onOpenLearnMoreAboutApproveClick THEN urlOpener opens approve url`() = runTest {
        every { urlOpener.openUrl(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onOpenLearnMoreAboutApproveClick()

        verify { urlOpener.openUrl("https://tangem.com/en/blog/post/give-revoke-permission/") }

        model.onDestroy()
    }
}