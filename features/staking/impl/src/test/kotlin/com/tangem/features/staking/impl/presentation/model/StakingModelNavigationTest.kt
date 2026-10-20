package com.tangem.features.staking.impl.presentation.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.converters.AmountAccountConverter
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.ethFee
import com.tangem.features.staking.impl.fixtures.polygonToken
import com.tangem.test.core.ProvideTestModels
import com.tangem.common.TangemBlogUrlBuilder
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelNavigationTest : StakingModelTestBase() {

    @Test
    fun `GIVEN loaded model WHEN onBackClick THEN router pop and stateController clear called`() = runTest {
        // Arrange
        every { stateController.value } returns initialUiState
        every { appRouter.pop(any()) } just Runs
        every { stateController.clear() } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onBackClick()

        // Assert
        verify { appRouter.pop(any()) }
        verify { stateController.clear() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN targets AND no yield balance WHEN onNextClick with balance THEN validators unavailable alert sent`() =
        runTest {
            // Arrange
            every { messageSender.send(any()) } just Runs
            every { testYield.areAllValidatorsFull } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onNextClick(balanceState = null)
            advanceUntilIdle()

            // Assert
            verify {
                messageSender.send(
                    match { it is DialogMessage }, // dialog from StakingModel.stakingEventFactory
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN partial amount disabled WHEN onNextClick with null balance THEN updateAll called with transformers`() =
        runTest {
            // Arrange
            every { stateController.value } returns initialUiState
            every { testYield.args.enter.isPartialAmountDisabled } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onNextClick(balanceState = null)
            advanceUntilIdle()

            // Assert
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
        // Arrange
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

        // Act
        model.onNextClick(balanceState = null)
        advanceUntilIdle()

        // Assert
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
            // Arrange
            val uiStateFlow = MutableStateFlow(initialUiState)
            every { stateController.uiState } returns uiStateFlow
            every {
                stakingOperationsFactory.createFeeLoader(
                    cryptoCurrencyStatusProvider = any(),
                    userWallet = any(),
                    integration = any(),
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

            // Act
            model.onPrevClick()

            // Assert
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
        // Arrange
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

        // Act
        model.onPrevClick()

        // Assert
        verify(exactly = 0) { stateController.update(any<Transformer<StakingUiState>>()) }
        verify(exactly = 0) { stateController.update(any<(StakingUiState) -> StakingUiState>()) }
        verify(exactly = 0) { stateController.updateAll(*anyVararg()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN completed state WHEN onPrevClick THEN onNextClick called`() = runTest {
        // Arrange
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

        // Act
        model.onPrevClick()
        advanceUntilIdle()

        // Assert
        verify {
            stateController.update(any<(StakingUiState) -> StakingUiState>())
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN non-confirmation step WHEN onPrevClick THEN stakingStateRouter onPrevClick called`() = runTest {
        // Arrange
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

        // Act
        model.onPrevClick()

        // Assert
        verify { stateController.update(any<(StakingUiState) -> StakingUiState>()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN loaded model WHEN onRefreshSwipe with true THEN loading set and balanceUpdater partialUpdate called`() =
        runTest {
            // Arrange
            val testAppScope = object : AppCoroutineScope,
                CoroutineScope by this {}

            val model = createModel(
                testScope = this,
                coroutineScope = testAppScope,
            )
            advanceUntilIdle()

            // Act
            model.onRefreshSwipe(isRefreshing = true)
            advanceUntilIdle()

            // Assert
            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> {
                        it is SetInitialLoadingStateTransformer
                    },
                )
            }
            coVerify { mockBalanceUpdater.partialUpdate() }

            model.onDestroy()
        }

    @Test
    fun `GIVEN refresh started WHEN balance update completes THEN refreshing shown and then hidden`() = runTest {
        // Arrange
        val testAppScope = object : AppCoroutineScope, CoroutineScope by this {}
        val model = createModel(testScope = this, coroutineScope = testAppScope)
        advanceUntilIdle()
        val transformers = mutableListOf<Transformer<StakingUiState>>()
        every { stateController.update(capture(transformers)) } just Runs
        val state = stakingUiState(initialInfoState = initialInfoData())

        // Act
        model.onRefreshSwipe(isRefreshing = true)
        advanceUntilIdle()

        // Assert
        val refreshingFlags = transformers.filterIsInstance<SetInitialLoadingStateTransformer>().map { transformer ->
            (transformer.transform(state).initialInfoState as StakingStates.InitialInfoState.Data)
                .pullToRefreshConfig.isRefreshing
        }
        assertThat(refreshingFlags).containsExactly(true, false).inOrder()

        model.onDestroy()
    }

    @Test
    fun `GIVEN failed token transaction WHEN onFailedTxEmailClick THEN blockchain error and email built from state`() =
        runTest {
            // Arrange
            val token = polygonToken()
            val accountStatus = accountCurrencyStatusOf(currencyStatus(currency = token))
            every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns
                flowOf(accountStatus)
            coEvery { getAllowanceUseCase(any(), any(), any()) } returns Either.Right(BigDecimal.ZERO)
            val chosenTarget = target()
            val uiState = stakingUiState(
                amountState = amountData(cryptoAmount = BigDecimal("1.5")),
                confirmationState = confirmationData(feeState = feeContent(ethFee(BigDecimal("0.01")))),
                validatorState = validatorData(chosenTarget = chosenTarget),
            )
            every { stateController.uiState } returns MutableStateFlow(uiState)
            val walletMetaInfo: WalletMetaInfo = mockk(relaxed = true)
            coEvery { getWalletMetaInfoUseCase(userWalletId = any()) } returns Either.Right(walletMetaInfo)
            every { saveBlockchainErrorUseCase(error = any()) } just Runs
            coEvery { sendFeedbackEmailUseCase(type = any()) } just Runs
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onFailedTxEmailClick("test error")
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                saveBlockchainErrorUseCase(
                    BlockchainErrorInfo(
                        errorMessage = "test error",
                        networkId = token.network.id,
                        destinationAddress = chosenTarget.address,
                        tokenSymbol = "POL",
                        amount = "1.5SOL",
                        fee = "0.01SOL",
                    ),
                )
            }
            coVerify(exactly = 1) {
                sendFeedbackEmailUseCase(
                    FeedbackEmailType.StakingProblem(
                        walletMetaInfo = walletMetaInfo,
                        validatorName = chosenTarget.name,
                        transactionTypes = emptyList(),
                        unsignedTransactions = emptyList(),
                    ),
                )
            }

            model.onDestroy()
        }

    @ParameterizedTest
    @MethodSource("provideAccountsModeModels")
    fun `GIVEN accounts mode setting WHEN onNextClick with partial amount disabled THEN amount title follows it`(
        model: AccountsModeModel,
    ) = runTest {
        // Arrange
        every { stateController.value } returns initialUiState
        every { testYield.args.enter.isPartialAmountDisabled } returns true
        val status = currencyStatus()
        val accountStatus = accountCurrencyStatusOf(status)
        every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns flowOf(accountStatus)
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns model.isAccountsModeEnabled
        val transformers = mutableListOf<Any?>()
        every { stateController.updateAll(*anyVararg()) } answers {
            args.forEach { arg -> if (arg is Array<*>) transformers.addAll(arg) else transformers.add(arg) }
        }
        val stakingModel = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        stakingModel.onNextClick(balanceState = null)
        advanceUntilIdle()

        // Assert
        val amountState = transformers.filterIsInstance<SetAmountDataTransformer>().single()
            .transform(stakingUiState()).amountState as AmountState.Data
        val expected = if (model.isAccountsModeEnabled) {
            AmountAccountConverter(
                prefixText = resourceReference(R.string.common_from),
                isAccountsMode = true,
                walletTitle = stringReference(""),
            ).convert(accountStatus.account)
        } else {
            AccountTitleUM.Text(stringReference(""))
        }
        assertThat(amountState.accountTitleUM).isEqualTo(expected)

        stakingModel.onDestroy()
    }

    data class AccountsModeModel(val isAccountsModeEnabled: Boolean)

    @Test
    fun `GIVEN partial amount enabled WHEN onNextClick with null balance THEN only confirmation init applied`() =
        runTest {
            // Arrange
            every { stateController.value } returns initialUiState
            every { testYield.args.enter.isPartialAmountDisabled } returns false
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onNextClick(balanceState = null)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { stateController.updateAll(match { it is SetConfirmationStateInitTransformer }) }
            verify(exactly = 0) {
                stateController.updateAll(match { it is SetConfirmationStateInitTransformer }, any(), any(), any())
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN confirmation step without confirmation data WHEN onPrevClick THEN nothing happens`() = runTest {
        // Arrange
        val state = stakingUiState(currentStep = StakingStep.Confirmation)
        every { stateController.uiState } returns MutableStateFlow(state)
        every { stateController.value } returns state
        val model = createModel(testScope = this)
        advanceUntilIdle()
        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)

        // Act
        model.onPrevClick()

        // Assert
        verify(exactly = 0) { stateController.update(any<(StakingUiState) -> StakingUiState>()) }
        verify(exactly = 0) { stateController.update(any<Transformer<StakingUiState>>()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN Exit action AND preferred targets WHEN onAmountEnterClick THEN validator not reset`() = runTest {
        // Arrange
        every { stateController.value } returns initialUiState
        every { initialUiState.actionType } returns StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)
        every { testYield.preferredValidators } returns listOf(mockk(relaxed = true))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountEnterClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { stateController.updateAll(match { it is ValidatorSelectChangeTransformer }) }

        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no transaction url WHEN explore or share clicked THEN nothing opened or shared`(model: NoTxUrlModel) =
        runTest {
            // Arrange
            every { stateController.uiState } returns
                MutableStateFlow(stakingUiState(confirmationState = model.confirmationState))
            val stakingModel = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            if (model.isShare) stakingModel.onShareClick() else stakingModel.onExploreClick()

            // Assert
            verify(exactly = 0) { innerRouter.openUrl(any()) }
            verify(exactly = 0) { shareManager.shareText(any()) }
            verify(exactly = 0) { vibratorHapticManager.performOneTime(any()) }

            stakingModel.onDestroy()
        }

    data class NoTxUrlModel(val isShare: Boolean, val confirmationState: StakingStates.ConfirmationState)

    @Test
    fun `GIVEN no amount fee or validator data WHEN onFailedTxEmailClick THEN error reported with empty fields`() =
        runTest {
            // Arrange
            val coinStatus = currencyStatus()
            val accountStatus = accountCurrencyStatusOf(coinStatus)
            every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns
                flowOf(accountStatus)
            every { stateController.uiState } returns MutableStateFlow(stakingUiState())
            val walletMetaInfo: WalletMetaInfo = mockk(relaxed = true)
            coEvery { getWalletMetaInfoUseCase(userWalletId = any()) } returns Either.Right(walletMetaInfo)
            every { saveBlockchainErrorUseCase(error = any()) } just Runs
            coEvery { sendFeedbackEmailUseCase(type = any()) } just Runs
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onFailedTxEmailClick("test error")
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                saveBlockchainErrorUseCase(
                    BlockchainErrorInfo(
                        errorMessage = "test error",
                        networkId = coinStatus.currency.network.id,
                        destinationAddress = "",
                        tokenSymbol = null,
                        amount = "",
                        fee = "",
                    ),
                )
            }
            coVerify(exactly = 1) {
                sendFeedbackEmailUseCase(
                    FeedbackEmailType.StakingProblem(
                        walletMetaInfo = walletMetaInfo,
                        validatorName = null,
                        transactionTypes = emptyList(),
                        unsignedTransactions = emptyList(),
                    ),
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN blog url WHEN onInitialInfoBannerClick THEN analytics sent and url opened`() = runTest {
        // Arrange
        val expectedUrl = "https://tangem.com/blog/post/how-to-stake-cryptocurrency/?utm_source=tangem-app"
        mockkObject(TangemBlogUrlBuilder)
        coEvery { TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.HowToStake) } returns expectedUrl
        every { innerRouter.openUrl(any()) } just Runs
        var model: StakingModel? = null

        try {
            model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onInitialInfoBannerClick()
            advanceUntilIdle()

            // Assert
            verify {
                analyticsEventHandler.send(match { it is StakingAnalyticsEvent.WhatIsStaking })
            }
            verify { innerRouter.openUrl(expectedUrl) }
        } finally {
            model?.onDestroy()
            unmockkObject(TangemBlogUrlBuilder)
        }
    }

    @Test
    fun `GIVEN loaded model WHEN onInfoClick THEN ShowInfoBottomSheetStateTransformer applied`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onInfoClick(InfoType.ANNUAL_PERCENTAGE_RATE)

        // Assert
        verify {
            stateController.update(
                match<Transformer<StakingUiState>> {
                    it is ShowInfoBottomSheetStateTransformer
                },
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN empty preferredTargets WHEN onAmountEnterClick THEN noAvailableValidators alert sent`() = runTest {
        // Arrange
        every { testYield.preferredValidators } returns emptyList()
        every { messageSender.send(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountEnterClick()

        // Assert
        verify { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN empty preferredTargets AND Exit action WHEN onAmountEnterClick THEN no alert sent`() = runTest {
        // Arrange
        every { stateController.value } returns initialUiState
        every { initialUiState.actionType } returns StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)
        every { testYield.preferredValidators } returns emptyList()
        every { messageSender.send(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountEnterClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN non-empty preferredTargets WHEN onAmountEnterClick THEN validator reset and onNextClick called`() =
        runTest {
            // Arrange
            every { stateController.value } returns initialUiState
            every { testYield.preferredValidators } returns listOf(mockk(relaxed = true))
            every { initialUiState.actionType } returns StakingActionCommonType.Enter(shouldSkipEnterAmount = false)
            every { testYield.args.enter.isPartialAmountDisabled } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onAmountEnterClick()
            advanceUntilIdle()

            // Assert
            verify {
                stateController.updateAll(
                    match { it is ValidatorSelectChangeTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN txUrl not null WHEN onExploreClick THEN analytics sent and url opened`() = runTest {
        // Arrange
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

        // Act
        model.onExploreClick()

        // Assert
        verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonExplore }) }
        verify { innerRouter.openUrl(txUrl) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN txUrl not null WHEN onShareClick THEN analytics sent and shareManager called`() = runTest {
        // Arrange
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

        // Act
        model.onShareClick()

        // Assert
        verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonShare }) }
        verify { vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click) }
        verify { shareManager.shareText(txUrl) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN wallet meta info WHEN onFailedTxEmailClick THEN analytics sent and sendFeedbackEmail called`() =
        runTest {
            // Arrange
            coEvery { getWalletMetaInfoUseCase(userWalletId = any()) } returns Either.Right(mockk(relaxed = true))
            every { saveBlockchainErrorUseCase(error = any()) } just Runs
            coEvery { sendFeedbackEmailUseCase(type = any()) } just Runs

            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onFailedTxEmailClick("test error")
            advanceUntilIdle()

            // Assert
            verify { analyticsEventHandler.send(match { it is Basic.ButtonSupport }) }
            coVerify { sendFeedbackEmailUseCase(match { it is FeedbackEmailType.StakingProblem }) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN loaded model WHEN openTokenDetails THEN innerRouter openTokenDetails called`() = runTest {
        // Arrange
        every { innerRouter.openTokenDetails(any(), any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val currency: CryptoCurrency = mockk(relaxed = true)

        // Act
        model.openTokenDetails(currency)

        // Assert
        verify { innerRouter.openTokenDetails(testUserWalletId, currency) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN loaded model WHEN showPrimaryClickAlert THEN messageSender sends alert`() = runTest {
        // Arrange
        every { messageSender.send(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.showPrimaryClickAlert()

        // Assert
        verify { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN approve blog url WHEN onOpenLearnMoreAboutApproveClick THEN urlOpener opens approve url`() = runTest {
        // Arrange
        val expectedUrl = "https://tangem.com/blog/post/give-revoke-permission/?utm_source=tangem-app"
        mockkObject(TangemBlogUrlBuilder)
        coEvery { TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.GiveRevokePermission) } returns expectedUrl
        every { urlOpener.openUrl(any()) } just Runs
        var model: StakingModel? = null

        try {
            model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onOpenLearnMoreAboutApproveClick()
            advanceUntilIdle()

            // Assert
            verify { urlOpener.openUrl(expectedUrl) }
        } finally {
            model?.onDestroy()
            unmockkObject(TangemBlogUrlBuilder)
        }
    }

    companion object {
        @JvmStatic
        fun provideAccountsModeModels() = listOf(
            AccountsModeModel(isAccountsModeEnabled = true),
            AccountsModeModel(isAccountsModeEnabled = false),
        )

        @JvmStatic
        fun provideTestModels() = listOf(
            NoTxUrlModel(isShare = false, confirmationState = StakingStates.ConfirmationState.Empty()),
            NoTxUrlModel(isShare = false, confirmationState = confirmationData()),
            NoTxUrlModel(isShare = true, confirmationState = StakingStates.ConfirmationState.Empty()),
            NoTxUrlModel(isShare = true, confirmationState = confirmationData()),
        )
    }
}