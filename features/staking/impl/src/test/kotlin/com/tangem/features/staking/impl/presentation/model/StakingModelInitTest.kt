package com.tangem.features.staking.impl.presentation.model

import arrow.core.Either
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.YieldBalanceItem
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.features.staking.impl.presentation.state.createAssentConfirmationUiState
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.test.core.ProvideTestModels
import com.tangem.features.staking.impl.presentation.state.helpers.StakingFeeLoader
import com.tangem.features.staking.impl.presentation.state.transformers.HideBalanceStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateLoadingTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.amount.AmountRoundToIntegerTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateResetAssentTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetInitialDataStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.confirmation.SetUpdatedAllowanceTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.notifications.AddStakingNotificationsTransformer
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.Transformer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelInitTest : StakingModelTestBase() {

    @Test
    fun `GIVEN currency status emitted WHEN model created THEN analytics sent and fee status fetched`() = runTest {
        // Arrange
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every { testCryptoCurrencyStatus.value } returns mockk {
            every { contributions } returns listOf(
                mockk<StakingBalance.Data.StakeKit> {
                    every { balance } returns YieldBalanceItem(
                        items = listOf(
                            mockk { every { validatorAddress } returns "address1" },
                            mockk { every { validatorAddress } returns "address2" },
                        ),
                        integrationId = "test",
                    )
                },
            )
        }
        every {
            getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
        } returns flowOf(testAccountCurrencyStatus)
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Left(mockk())

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify {
            paramsInterceptorHolder.addParamsInterceptor(
                match { it.id() == "StakingParamsInterceptorId" },
            )
        }
        verify {
            analyticsEventHandler.send(
                StakingAnalyticsEvent.StakingInfoScreenOpened(
                    validatorsCount = 2,
                ),
            )
        }
        verify {
            stateController.initializeWithUserWallet(testUserWallet)
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN currency status emitted twice WHEN model created THEN analytics sent only once`() = runTest {
        // Arrange
        val statusFlow = MutableSharedFlow<com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus>()
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every {
            getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
        } returns statusFlow
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Left(mockk())

        // Act
        val model = createModel(testScope = this)
        statusFlow.emit(testAccountCurrencyStatus)
        advanceUntilIdle()
        statusFlow.emit(testAccountCurrencyStatus)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                event = StakingAnalyticsEvent.StakingInfoScreenOpened(validatorsCount = 0),
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN account initialized WHEN checkForTonHeatupCase THEN no error logged`() = runTest {
        // Arrange
        mockkObject(TangemLogger)
        var model: StakingModel? = null

        try {
            // Act
            model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            coVerify { checkAccountInitializedUseCase(testUserWalletId, any()) }
            verify(exactly = 0) { TangemLogger.e(any(), any()) }
        } finally {
            model?.onDestroy()
            unmockkObject(TangemLogger)
        }
    }

    @Test
    fun `GIVEN checkAccountInitialized fails WHEN checkForTonHeatupCase THEN error logged`() = runTest {
        // Arrange
        val testError = RuntimeException("network error")
        coEvery {
            checkAccountInitializedUseCase(testUserWalletId, any())
        } returns Either.Left(testError)
        mockkObject(TangemLogger)
        every { TangemLogger.e(any(), any()) } just Runs
        var model: StakingModel? = null

        try {
            // Act
            model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            verify { TangemLogger.e("Error", testError) }
        } finally {
            model?.onDestroy()
            unmockkObject(TangemLogger)
        }
    }

    @Test
    fun `GIVEN approval needed WHEN setupApprovalNeeded THEN getAllowanceUseCase called`() = runTest {
        // Arrange
        val spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"
        mockkObject(StakingIntegrationID.Companion)
        every {
            StakingIntegrationID.create(any())
        } returns mockk {
            every { approval } returns StakingApproval.Needed(spenderAddress)
        }
        coEvery {
            getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
        } returns Either.Right(BigDecimal.TEN)
        var model: StakingModel? = null

        try {
            // Act
            model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            coVerify {
                getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
            }
        } finally {
            model?.onDestroy()
            unmockkObject(StakingIntegrationID.Companion)
        }
    }

    @Test
    fun `GIVEN approval needed AND getAllowance fails WHEN setupApprovalNeeded THEN no crash`() = runTest {
        // Arrange
        val spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"
        mockkObject(StakingIntegrationID.Companion)
        every {
            StakingIntegrationID.create(any())
        } returns mockk {
            every { approval } returns StakingApproval.Needed(spenderAddress)
        }
        coEvery {
            getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
        } returns Either.Left(RuntimeException("allowance error"))
        var model: StakingModel? = null

        try {
            // Act
            model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            coVerify {
                getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
            }
        } finally {
            model?.onDestroy()
            unmockkObject(StakingIntegrationID.Companion)
        }
    }

    @Test
    fun `GIVEN any token staked WHEN setupIsAnyTokenStaked THEN use case called with correct wallet id`() = runTest {
        // Arrange
        coEvery { isAnyTokenStakedUseCase(testUserWalletId) } returns Either.Right(true)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        coVerify { isAnyTokenStakedUseCase(testUserWalletId) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN subtract available WHEN checkIfSubtractAvailable THEN use case called with correct params`() = runTest {
        // Arrange
        coEvery {
            isAmountSubtractAvailableUseCase(testUserWalletId, any())
        } returns Either.Right(true)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        coVerify { isAmountSubtractAvailableUseCase(testUserWalletId, any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN actions emitted WHEN subscribeOnActionsUpdates AND isInitState THEN updateInitialData`() = runTest {
        // Arrange
        val testActions = listOf(mockk<StakingAction>(relaxed = true))
        every {
            getActionsUseCase(testUserWalletId, any())
        } returns flowOf(Either.Right(testActions))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(atLeast = 1) {
            stateController.updateAll(
                match { it is SetInitialDataStateTransformer },
                any(),
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN init step WHEN subscribeOnStepChanges THEN updateInitialData and partialUpdate called`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(atLeast = 1) {
            stateController.updateAll(
                match { it is SetInitialDataStateTransformer },
                any(),
            )
        }
        coVerify { mockBalanceUpdater.partialUpdate() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN assent step AND isWarning WHEN subscribeOnStepChanges THEN getFee AND amount rounded to integer`() =
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
            } returns mockk<StakingFeeLoader> {
                coEvery {
                    getFee(any(), any(), any(), any())
                } just Runs
            }

            val model = createModel(testScope = this)
            advanceUntilIdle()

            val assentUiState = mockk<StakingUiState>(relaxed = true) {
                every { currentStep } returns StakingStep.Confirmation
                every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data> {
                    every { innerState } returns InnerConfirmationStakingState.ASSENT
                }
                every { amountState } returns mockk<AmountState.Data> {
                    every { amountTextField } returns mockk {
                        every { isWarning } returns true
                    }
                }
            }

            // Act
            uiStateFlow.value = assentUiState
            advanceUntilIdle()

            // Assert
            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is SetConfirmationStateLoadingTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN balance hidden WHEN subscribeOnBalanceHiding THEN HideBalanceStateTransformer applied`() = runTest {
        // Arrange
        val balanceHidingSettings = BalanceHidingSettings(
            isHidingEnabledInSettings = true,
            isBalanceHidden = true,
            isBalanceHidingNotificationEnabled = false,
        )
        every { getBalanceHidingSettingsUseCase() } returns flowOf(balanceHidingSettings)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify {
            stateController.update(
                match<Transformer<StakingUiState>> { it is HideBalanceStateTransformer },
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN assent confirmation WHEN refreshed status emitted THEN notifications recomputed with refreshed status`() =
        runTest {
            // Arrange
            val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
            val uiStateFlow = MutableStateFlow(initialUiState)
            val (cachedStatus, cachedAccountStatus) = createMockedAccountCurrencyStatus()
            val (refreshedStatus, refreshedAccountStatus) = createMockedAccountCurrencyStatus()
            arrangeStatusRefreshOnConfirmation(statusFlow, uiStateFlow, cachedStatus)

            val model = createModel(testScope = this)
            statusFlow.emit(cachedAccountStatus)
            advanceUntilIdle()
            uiStateFlow.value = createConfirmationUiState(InnerConfirmationStakingState.ASSENT)
            advanceUntilIdle()

            // Act
            statusFlow.emit(refreshedAccountStatus)
            advanceUntilIdle()

            // Assert
            coVerify {
                getCurrencyCheckUseCase(
                    userWalletId = any(),
                    currencyStatus = refreshedStatus,
                    feeCurrencyStatus = any(),
                    amount = any(),
                    fee = any(),
                    feeCurrencyBalanceAfterTransaction = any(),
                    recipientAddress = any(),
                )
            }
            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is AddStakingNotificationsTransformer },
                )
            }
            coVerify(exactly = 0) { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), refreshedStatus) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN confirmation in progress WHEN refreshed status emitted THEN status update ignored`() = runTest {
        // Arrange
        val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
        val uiStateFlow = MutableStateFlow(initialUiState)
        val (cachedStatus, cachedAccountStatus) = createMockedAccountCurrencyStatus()
        val (refreshedStatus, refreshedAccountStatus) = createMockedAccountCurrencyStatus()
        arrangeStatusRefreshOnConfirmation(statusFlow, uiStateFlow, cachedStatus)

        val model = createModel(testScope = this)
        statusFlow.emit(cachedAccountStatus)
        advanceUntilIdle()
        uiStateFlow.value = createConfirmationUiState(InnerConfirmationStakingState.IN_PROGRESS)
        advanceUntilIdle()

        // Act
        statusFlow.emit(refreshedAccountStatus)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) {
            getCurrencyCheckUseCase(
                userWalletId = any(),
                currencyStatus = refreshedStatus,
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = any(),
                recipientAddress = any(),
            )
        }
        coVerify(exactly = 0) { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), refreshedStatus) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN approval sent WHEN refreshed status emitted on assent THEN rebuilt notifications keep approval in progress`() =
        runTest {
            // Arrange
            val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
            val uiStateFlow = MutableStateFlow(initialUiState)
            val (cachedStatus, cachedAccountStatus) = createMockedAccountCurrencyStatus()
            val (_, refreshedAccountStatus) = createMockedAccountCurrencyStatus()
            arrangeStatusRefreshOnConfirmation(statusFlow, uiStateFlow, cachedStatus)
            every { cachedStatus.currency } returns mockk<CryptoCurrency.Token>(relaxed = true)
            mockkObject(StakingIntegrationID.Companion)
            every { StakingIntegrationID.create(any()) } returns mockk {
                every { approval } returns StakingApproval.Needed("0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908")
            }
            coEvery { getAllowanceUseCase(any(), any(), any()) } returns Either.Right(BigDecimal.ZERO)

            var model: StakingModel? = null
            try {
                model = createModel(testScope = this)
                runCurrent()
                statusFlow.emit(cachedAccountStatus)
                advanceUntilIdle()
                uiStateFlow.value = createConfirmationUiState(InnerConfirmationStakingState.ASSENT).apply {
                    every { amountState } returns mockk<AmountState.Data>(relaxed = true) {
                        every { amountTextField.cryptoAmount.value } returns BigDecimal.ONE
                    }
                }
                advanceUntilIdle()
                val transformers = mutableListOf<Transformer<StakingUiState>>()
                every { stateController.update(capture(transformers)) } just Runs

                // Act — runCurrent, not advanceUntilIdle: the allowance poll repeats forever while allowance stays 0
                model.getApprovalParams()!!.callback.onApproveDone()
                runCurrent()
                statusFlow.emit(refreshedAccountStatus)
                runCurrent()

                // Assert
                val rebuilt = transformers.filterIsInstance<AddStakingNotificationsTransformer>().last()
                    .transform(createAssentConfirmationUiState())
                val confirmation = rebuilt.confirmationState as StakingStates.ConfirmationState.Data
                assertThat(confirmation.notifications)
                    .contains(StakingNotification.Warning.TransactionInProgress.Approval)
            } finally {
                model?.onDestroy()
                unmockkObject(StakingIntegrationID.Companion)
            }
        }

    @Test
    fun `GIVEN approval in progress WHEN approval fails THEN assent reset and rebuilt notifications drop approval`() =
        runTest {
            // Arrange
            val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
            val uiStateFlow = MutableStateFlow(initialUiState)
            val (cachedStatus, cachedAccountStatus) = createMockedAccountCurrencyStatus()
            val (_, refreshedAccountStatus) = createMockedAccountCurrencyStatus()
            arrangeApprovalOnConfirmation(statusFlow, uiStateFlow, cachedStatus, allowance = BigDecimal.ZERO)

            var model: StakingModel? = null
            try {
                model = createModel(testScope = this)
                val callback = moveToApprovalAssent(model, statusFlow, cachedAccountStatus, uiStateFlow)
                callback.onApproveDone()
                runCurrent()
                val transformers = mutableListOf<Transformer<StakingUiState>>()
                every { stateController.update(capture(transformers)) } just Runs

                // Act
                callback.onApproveFailed()
                statusFlow.emit(refreshedAccountStatus)
                runCurrent()

                // Assert
                assertThat(transformers.filterIsInstance<SetConfirmationStateResetAssentTransformer>()).hasSize(1)
                val rebuilt = transformers.filterIsInstance<AddStakingNotificationsTransformer>().last()
                    .transform(createAssentConfirmationUiState())
                val confirmation = rebuilt.confirmationState as StakingStates.ConfirmationState.Data
                assertThat(confirmation.notifications)
                    .doesNotContain(StakingNotification.Warning.TransactionInProgress.Approval)
            } finally {
                model?.onDestroy()
                unmockkObject(StakingIntegrationID.Companion)
            }
        }

    @Test
    fun `GIVEN allowance covers amount WHEN approval done THEN allowance polling stops after first success`() =
        runTest {
            // Arrange
            val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
            val uiStateFlow = MutableStateFlow(initialUiState)
            val (cachedStatus, cachedAccountStatus) = createMockedAccountCurrencyStatus()
            arrangeApprovalOnConfirmation(statusFlow, uiStateFlow, cachedStatus, allowance = BigDecimal.TEN)

            var model: StakingModel? = null
            try {
                model = createModel(testScope = this)
                val callback = moveToApprovalAssent(model, statusFlow, cachedAccountStatus, uiStateFlow)

                // Act
                callback.onApproveDone()
                runCurrent()
                advanceTimeBy(ALLOWANCE_POLL_PERIOD_MS + 1)
                runCurrent()

                // Assert
                coVerify(exactly = 2) { getAllowanceUseCase(any(), any(), any()) }
                verify(exactly = 1) {
                    stateController.update(match<Transformer<StakingUiState>> { it is SetUpdatedAllowanceTransformer })
                }
            } finally {
                model?.onDestroy()
                unmockkObject(StakingIntegrationID.Companion)
            }
        }

    @Test
    fun `GIVEN uninitialized TON account WHEN status refreshed on confirmation THEN notifications recomputed`() =
        runTest {
            // Arrange
            val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
            val uiStateFlow = MutableStateFlow(initialUiState)
            every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns statusFlow
            every { stateController.uiState } returns uiStateFlow
            coEvery { checkAccountInitializedUseCase(testUserWalletId, any()) } returns false.right()
            stubNotificationUseCases()
            val ton = coin(Blockchain.TON)
            val cachedAccountStatus = accountCurrencyStatusOf(currencyStatus(currency = ton, amount = BigDecimal.ONE))
            val refreshedAccountStatus = accountCurrencyStatusOf(currencyStatus(currency = ton, amount = BigDecimal.TEN))

            val model = createModel(testScope = this)
            runCurrent()
            statusFlow.emit(cachedAccountStatus)
            advanceUntilIdle()
            uiStateFlow.value = createConfirmationUiState(InnerConfirmationStakingState.IN_PROGRESS)
            advanceUntilIdle()
            val transformers = mutableListOf<Transformer<StakingUiState>>()
            every { stateController.update(capture(transformers)) } just Runs

            // Act
            statusFlow.emit(refreshedAccountStatus)
            advanceUntilIdle()

            // Assert
            val notificationsTransformer = transformers.filterIsInstance<AddStakingNotificationsTransformer>().single()
            val confirmation = notificationsTransformer
                .transform(createAssentConfirmationUiState(StakingActionCommonType.Enter(shouldSkipEnterAmount = false)))
                .confirmationState as StakingStates.ConfirmationState.Data
            assertThat(confirmation.notifications.single())
                .isInstanceOf(StakingNotification.Warning.InitializeTonAccount::class.java)

            model.onDestroy()
        }

    @Test
    fun `GIVEN balance refreshed on first init step WHEN user returns to init step THEN balance not refreshed again`() =
        runTest {
            // Arrange
            val uiStateFlow = MutableStateFlow(initialUiState)
            every { stateController.uiState } returns uiStateFlow
            val returnedInitialUiState = mockk<StakingUiState>(relaxed = true) {
                every { currentStep } returns StakingStep.InitialInfo
            }
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            uiStateFlow.value = mockk(relaxed = true) { every { currentStep } returns StakingStep.Amount }
            advanceUntilIdle()
            uiStateFlow.value = returnedInitialUiState
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { mockBalanceUpdater.partialUpdate() }
            verify(exactly = 2) {
                stateController.updateAll(match { it is SetInitialDataStateTransformer }, any())
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN init step WHEN actions emitted after load THEN initial data rebuilt once more`() = runTest {
        // Arrange
        val actionsFlow = MutableSharedFlow<Either<StakingError, List<StakingAction>>>()
        every { getActionsUseCase(testUserWalletId, any()) } returns actionsFlow
        val model = createModel(testScope = this)
        advanceUntilIdle()
        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)

        // Act
        actionsFlow.emit(Either.Right(listOf(mockk(relaxed = true))))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            stateController.updateAll(match { it is SetInitialDataStateTransformer }, any())
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN allowance equal to amount WHEN approval done THEN updated allowance applied`() = runTest {
        // Arrange
        val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
        val uiStateFlow = MutableStateFlow(initialUiState)
        val (cachedStatus, cachedAccountStatus) = createMockedAccountCurrencyStatus()
        arrangeApprovalOnConfirmation(statusFlow, uiStateFlow, cachedStatus, allowance = BigDecimal.ONE)

        var model: StakingModel? = null
        try {
            model = createModel(testScope = this)
            val callback = moveToApprovalAssent(model, statusFlow, cachedAccountStatus, uiStateFlow)

            // Act
            callback.onApproveDone()
            runCurrent()

            // Assert
            verify(exactly = 1) {
                stateController.update(match<Transformer<StakingUiState>> { it is SetUpdatedAllowanceTransformer })
            }
        } finally {
            model?.onDestroy()
            unmockkObject(StakingIntegrationID.Companion)
        }
    }

    @Test
    fun `GIVEN approval needed AND no fee currency status WHEN getApprovalParams THEN returns null`() = runTest {
        // Arrange
        val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
        val uiStateFlow = MutableStateFlow(initialUiState)
        val (cachedStatus, cachedAccountStatus) = createMockedAccountCurrencyStatus()
        arrangeApprovalOnConfirmation(statusFlow, uiStateFlow, cachedStatus, allowance = BigDecimal.ZERO)
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, cachedStatus) } returns Either.Right(null)

        var model: StakingModel? = null
        try {
            model = createModel(testScope = this)
            runCurrent()
            statusFlow.emit(cachedAccountStatus)
            advanceUntilIdle()
            uiStateFlow.value = createConfirmationUiState(InnerConfirmationStakingState.ASSENT).apply {
                every { amountState } returns mockk<AmountState.Data>(relaxed = true)
            }
            advanceUntilIdle()

            // Act
            val actual = model.getApprovalParams()

            // Assert
            assertThat(actual).isNull()
        } finally {
            model?.onDestroy()
            unmockkObject(StakingIntegrationID.Companion)
        }
    }

    @Test
    fun `GIVEN two different statuses on init step WHEN emitted THEN info screen analytics sent once`() = runTest {
        // Arrange
        val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
        every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns statusFlow
        val firstAccountStatus = accountCurrencyStatusOf(currencyStatus(amount = BigDecimal.ONE))
        val secondAccountStatus = accountCurrencyStatusOf(currencyStatus(amount = BigDecimal.TEN))
        val model = createModel(testScope = this)
        runCurrent()

        // Act
        statusFlow.emit(firstAccountStatus)
        advanceUntilIdle()
        statusFlow.emit(secondAccountStatus)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(StakingAnalyticsEvent.StakingInfoScreenOpened(validatorsCount = 0))
        }
        coVerify(exactly = 1) { getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, secondAccountStatus.status) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN balance hidden setting WHEN model created THEN balance hidden flow updated`() = runTest {
        // Arrange
        every { getBalanceHidingSettingsUseCase() } returns flowOf(
            BalanceHidingSettings(
                isHidingEnabledInSettings = true,
                isBalanceHidden = true,
                isBalanceHidingNotificationEnabled = false,
            ),
        )

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isBalanceHiddenFlow.value).isTrue()

        model.onDestroy()
    }

    @Test
    fun `GIVEN assent step without amount data WHEN user returns to init step THEN initial data rebuilt`() =
        runTest {
            // Arrange
            val uiStateFlow = MutableStateFlow(initialUiState)
            every { stateController.uiState } returns uiStateFlow
            every {
                stakingOperationsFactory.createFeeLoader(any(), any(), any())
            } returns mockk<StakingFeeLoader> { coEvery { getFee(any(), any(), any(), any()) } just Runs }
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            uiStateFlow.value = createConfirmationUiState(InnerConfirmationStakingState.ASSENT).apply {
                every { amountState } returns AmountState.Empty
            }
            advanceUntilIdle()
            uiStateFlow.value = mockk(relaxed = true) { every { currentStep } returns StakingStep.InitialInfo }
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) {
                stateController.update(match<Transformer<StakingUiState>> { it is AmountRoundToIntegerTransformer })
            }
            verify(exactly = 2) {
                stateController.updateAll(match { it is SetInitialDataStateTransformer }, any())
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN assent step with warning amount WHEN step entered THEN amount rounded to integer`() = runTest {
        // Arrange
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockk<StakingFeeLoader> { coEvery { getFee(any(), any(), any(), any()) } just Runs }
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        uiStateFlow.value = createConfirmationUiState(InnerConfirmationStakingState.ASSENT).apply {
            every { amountState } returns amountData(isWarning = true)
        }
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            stateController.update(match<Transformer<StakingUiState>> { it is AmountRoundToIntegerTransformer })
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN amount step WHEN actions emitted THEN initial data not rebuilt`() = runTest {
        // Arrange
        val actionsFlow = MutableSharedFlow<Either<StakingError, List<StakingAction>>>()
        every { getActionsUseCase(testUserWalletId, any()) } returns actionsFlow
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        val model = createModel(testScope = this)
        advanceUntilIdle()
        uiStateFlow.value = mockk(relaxed = true) { every { currentStep } returns StakingStep.Amount }
        advanceUntilIdle()
        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)

        // Act
        actionsFlow.emit(Either.Right(listOf(mockk(relaxed = true))))
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { stateController.updateAll(*anyVararg()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN init step WHEN actions request fails THEN initial data not rebuilt`() = runTest {
        // Arrange
        val actionsFlow = MutableSharedFlow<Either<StakingError, List<StakingAction>>>()
        every { getActionsUseCase(testUserWalletId, any()) } returns actionsFlow
        val model = createModel(testScope = this)
        advanceUntilIdle()
        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)

        // Act
        actionsFlow.emit(Either.Left(mockk(relaxed = true)))
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { stateController.updateAll(*anyVararg()) }

        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN token staking status WHEN initial data built THEN staking banner shown only when nothing staked`(
        model: AnyTokenStakedModel,
    ) = runTest {
        // Arrange
        val status = currencyStatus()
        val accountStatus = accountCurrencyStatusOf(status)
        every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns flowOf(accountStatus)
        coEvery { isAnyTokenStakedUseCase(testUserWalletId) } returns Either.Right(model.isAnyTokenStaked)
        every { invalidatePendingTransactionsUseCase(any(), any(), any()) } returns Either.Right(emptyList())
        val transformers = mutableListOf<Any?>()
        every { stateController.updateAll(*anyVararg()) } answers {
            args.forEach { arg -> if (arg is Array<*>) transformers.addAll(arg) else transformers.add(arg) }
        }

        // Act
        val stakingModel = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val initialInfo = transformers.filterIsInstance<SetInitialDataStateTransformer>().first()
            .transform(stakingUiState()).initialInfoState as StakingStates.InitialInfoState.Data
        assertThat(initialInfo.isBannerVisible).isEqualTo(!model.isAnyTokenStaked)

        stakingModel.onDestroy()
    }

    data class AnyTokenStakedModel(val isAnyTokenStaked: Boolean)

    private fun arrangeApprovalOnConfirmation(
        statusFlow: MutableSharedFlow<AccountCryptoCurrencyStatus>,
        uiStateFlow: MutableStateFlow<StakingUiState>,
        cachedStatus: CryptoCurrencyStatus,
        allowance: BigDecimal,
    ) {
        arrangeStatusRefreshOnConfirmation(statusFlow, uiStateFlow, cachedStatus)
        every { cachedStatus.currency } returns mockk<CryptoCurrency.Token>(relaxed = true)
        mockkObject(StakingIntegrationID.Companion)
        every { StakingIntegrationID.create(any()) } returns mockk {
            every { approval } returns StakingApproval.Needed("0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908")
        }
        coEvery { getAllowanceUseCase(any(), any(), any()) } returns Either.Right(allowance)
    }

    private suspend fun TestScope.moveToApprovalAssent(
        model: StakingModel,
        statusFlow: MutableSharedFlow<AccountCryptoCurrencyStatus>,
        cachedAccountStatus: AccountCryptoCurrencyStatus,
        uiStateFlow: MutableStateFlow<StakingUiState>,
    ): GiveApprovalComponent.Callback {
        runCurrent()
        statusFlow.emit(cachedAccountStatus)
        advanceUntilIdle()
        uiStateFlow.value = createConfirmationUiState(InnerConfirmationStakingState.ASSENT).apply {
            every { amountState } returns mockk<AmountState.Data>(relaxed = true) {
                every { amountTextField.cryptoAmount.value } returns BigDecimal.ONE
            }
        }
        advanceUntilIdle()
        return model.getApprovalParams()!!.callback
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RefreshedStatusBeforeConfirmation {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN step before confirmation WHEN refreshed status emitted THEN notifications recomputed`(
            model: StepModel,
        ) = runTest {
            // Arrange
            val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
            val uiStateFlow = MutableStateFlow(initialUiState)
            val (cachedStatus, cachedAccountStatus) = createMockedAccountCurrencyStatus()
            val (refreshedStatus, refreshedAccountStatus) = createMockedAccountCurrencyStatus()
            arrangeStatusRefreshOnConfirmation(statusFlow, uiStateFlow, cachedStatus)

            val stakingModel = createModel(testScope = this)
            statusFlow.emit(cachedAccountStatus)
            advanceUntilIdle()
            uiStateFlow.value = createUiStateOnStep(model.step)
            advanceUntilIdle()

            // Act
            statusFlow.emit(refreshedAccountStatus)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = model.expectedRecomputations) {
                getCurrencyCheckUseCase(
                    userWalletId = any(),
                    currencyStatus = refreshedStatus,
                    feeCurrencyStatus = any(),
                    amount = any(),
                    fee = any(),
                    feeCurrencyBalanceAfterTransaction = any(),
                    recipientAddress = any(),
                )
            }
            coVerify(exactly = 0) { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), refreshedStatus) }

            stakingModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            StepModel(step = StakingStep.Amount, expectedRecomputations = 1),
            StepModel(step = StakingStep.RewardsValidators, expectedRecomputations = 1),
            StepModel(step = StakingStep.RestakeValidator, expectedRecomputations = 1),
            StepModel(step = StakingStep.Validators, expectedRecomputations = 1),
            StepModel(step = StakingStep.Success, expectedRecomputations = 0),
        )
    }

    internal data class StepModel(val step: StakingStep, val expectedRecomputations: Int)

    private fun arrangeStatusRefreshOnConfirmation(
        statusFlow: MutableSharedFlow<AccountCryptoCurrencyStatus>,
        uiStateFlow: MutableStateFlow<StakingUiState>,
        cachedStatus: CryptoCurrencyStatus,
    ) {
        every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns statusFlow
        every { stateController.uiState } returns uiStateFlow
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, cachedStatus)
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getMinimumTransactionAmountSyncUseCase(testUserWalletId, cachedStatus)
        } returns Either.Left(mockk())
        coEvery {
            getBalanceNotEnoughForFeeWarningUseCase(
                fee = any(),
                userWalletId = any(),
                tokenStatus = any(),
                feeStatus = any(),
            )
        } returns Either.Right(null)
        coEvery {
            getCurrencyCheckUseCase(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = any(),
                recipientAddress = any(),
            )
        } returns mockk(relaxed = true)
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockk<StakingFeeLoader> {
            coEvery { getFee(any(), any(), any(), any()) } just Runs
        }
    }

    private fun createUiStateOnStep(step: StakingStep): StakingUiState = mockk(relaxed = true) {
        every { currentStep } returns step
    }

    private fun createConfirmationUiState(confirmationInnerState: InnerConfirmationStakingState): StakingUiState =
        mockk(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns confirmationInnerState
            }
        }

    @Test
    fun `GIVEN loaded model WHEN onDestroy THEN params interceptor removed`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onDestroy()

        // Assert
        verify {
            paramsInterceptorHolder.removeParamsInterceptor("StakingParamsInterceptorId")
        }
    }

    companion object {
        private const val ALLOWANCE_POLL_PERIOD_MS = 10_000L

        @JvmStatic
        fun provideTestModels() = listOf(
            AnyTokenStakedModel(isAnyTokenStaked = true),
            AnyTokenStakedModel(isAnyTokenStaked = false),
        )
    }
}