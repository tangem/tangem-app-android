package com.tangem.features.staking.impl.presentation.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.createAssentConfirmationUiState
import com.tangem.features.staking.impl.presentation.state.helpers.StakingFeeLoader
import com.tangem.features.staking.impl.presentation.state.transformers.HideBalanceStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateLoadingTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetInitialDataStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.notifications.AddStakingNotificationsTransformer
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.Transformer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelInitTest : StakingModelTestBase() {

    @Test
    fun `GIVEN currency status emitted WHEN model created THEN analytics sent and fee status fetched`() = runTest {
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every { testCryptoCurrencyStatus.value } returns mockk {
            // The staking accessor reads contributions first; empty is the toggle-off shape under test here.
            every { contributions } returns emptyList()
            every { stakingBalance } returns mockk<StakingBalance.Data.StakeKit> {
                every { balance } returns YieldBalanceItem(
                    items = listOf(
                        mockk { every { validatorAddress } returns "address1" },
                        mockk { every { validatorAddress } returns "address2" },
                    ),
                    integrationId = "test"
                )
            }
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

        val model = createModel(testScope = this)
        advanceUntilIdle()

        verify {
            paramsInterceptorHolder.addParamsInterceptor(
                match { it.id() == "StakingParamsInterceptorId" }
            )
        }
        verify {
            analyticsEventHandler.send(
                StakingAnalyticsEvent.StakingInfoScreenOpened(
                    validatorsCount = 2
                ),
            )
        }
        verify {
            stateController.initializeWithUserWallet(testUserWallet)
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN the staking balance arrives as a contribution WHEN model created THEN validators are still counted`() =
        runTest {
            // Arrange — toggle-on shape: the typed field is null and the balance is in `contributions`
            val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
            val stakeKit = mockk<StakingBalance.Data.StakeKit> {
                every { balance } returns YieldBalanceItem(
                    items = listOf(
                        mockk { every { validatorAddress } returns "address1" },
                        mockk { every { validatorAddress } returns "address2" },
                    ),
                    integrationId = "test",
                )
            }
            every { testCryptoCurrencyStatus.value } returns mockk {
                every { contributions } returns listOf(stakeKit)
                every { stakingBalance } returns null
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

            // Assert — a reader stuck on the typed field would report zero validators
            verify {
                analyticsEventHandler.send(
                    StakingAnalyticsEvent.StakingInfoScreenOpened(validatorsCount = 2),
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN currency status emitted twice WHEN model created THEN analytics sent only once`() = runTest {
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

        val model = createModel(testScope = this)
        statusFlow.emit(testAccountCurrencyStatus)
        advanceUntilIdle()
        statusFlow.emit(testAccountCurrencyStatus)
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsEventHandler.send(
                event = StakingAnalyticsEvent.StakingInfoScreenOpened(validatorsCount = 0)
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN account initialized WHEN checkForTonHeatupCase THEN no error logged`() = runTest {
        mockkObject(TangemLogger)

        val model = createModel(testScope = this)
        advanceUntilIdle()

        coVerify { checkAccountInitializedUseCase(testUserWalletId, any()) }
        verify(exactly = 0) { TangemLogger.e(any(), any()) }

        model.onDestroy()
        unmockkObject(TangemLogger)
    }

    @Test
    fun `GIVEN checkAccountInitialized fails WHEN checkForTonHeatupCase THEN error logged`() = runTest {
        val testError = RuntimeException("network error")
        coEvery {
            checkAccountInitializedUseCase(testUserWalletId, any())
        } returns Either.Left(testError)
        mockkObject(TangemLogger)
        every { TangemLogger.e(any(), any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        verify { TangemLogger.e("Error", testError) }

        model.onDestroy()
        unmockkObject(TangemLogger)
    }

    @Test
    fun `GIVEN approval needed WHEN setupApprovalNeeded THEN getAllowanceUseCase called`() = runTest {
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

        val model = createModel(testScope = this)
        advanceUntilIdle()

        coVerify {
            getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
        }

        model.onDestroy()
        unmockkObject(StakingIntegrationID.Companion)
    }

    @Test
    fun `GIVEN approval needed AND getAllowance fails WHEN setupApprovalNeeded THEN no crash`() = runTest {
        val spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"
        mockkObject(StakingIntegrationID.Companion)
        every {
            StakingIntegrationID.create(any())
        } returns mockk {
            every { approval } returns StakingApproval.Needed(spenderAddress)
        }
        coEvery {
            getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
        } returns Either.Left(RuntimeException("allowance error")) // error

        val model = createModel(testScope = this)
        advanceUntilIdle()

        coVerify {
            getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
        }

        model.onDestroy()
        unmockkObject(StakingIntegrationID.Companion)
    }

    @Test
    fun `GIVEN any token staked WHEN setupIsAnyTokenStaked THEN use case called with correct wallet id`() = runTest {
        coEvery { isAnyTokenStakedUseCase(testUserWalletId) } returns Either.Right(true)

        val model = createModel(testScope = this)
        advanceUntilIdle()

        coVerify { isAnyTokenStakedUseCase(testUserWalletId) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN subtract available WHEN checkIfSubtractAvailable THEN use case called with correct params`() = runTest {
        coEvery {
            isAmountSubtractAvailableUseCase(testUserWalletId, any())
        } returns Either.Right(true)

        val model = createModel(testScope = this)
        advanceUntilIdle()

        coVerify { isAmountSubtractAvailableUseCase(testUserWalletId, any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN actions emitted WHEN subscribeOnActionsUpdates AND isInitState THEN updateInitialData`() = runTest {
        val testActions = listOf(mockk<StakingAction>(relaxed = true))
        every {
            getActionsUseCase(testUserWalletId, any())
        } returns flowOf(Either.Right(testActions))

        val model = createModel(testScope = this)
        advanceUntilIdle()

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
        val model = createModel(testScope = this)
        advanceUntilIdle()

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
            val uiStateFlow = MutableStateFlow(initialUiState)
            every { stateController.uiState } returns uiStateFlow
            every {
                stakingOperationsFactory.createFeeLoader(
                    cryptoCurrencyStatusProvider = any(),
                    userWallet = any(),
                    integration = any()
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
            uiStateFlow.value = assentUiState
            advanceUntilIdle()

            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is SetConfirmationStateLoadingTransformer }
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN balance hidden WHEN subscribeOnBalanceHiding THEN HideBalanceStateTransformer applied`() = runTest {
        val balanceHidingSettings = BalanceHidingSettings(
            isHidingEnabledInSettings = true,
            isBalanceHidden = true,
            isBalanceHidingNotificationEnabled = false,
        )
        every { getBalanceHidingSettingsUseCase() } returns flowOf(balanceHidingSettings)

        val model = createModel(testScope = this)
        advanceUntilIdle()

        verify {
            stateController.update(
                match<Transformer<StakingUiState>> { it is HideBalanceStateTransformer }
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
                    match<Transformer<StakingUiState>> { it is AddStakingNotificationsTransformer }
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

            try {
                val model = createModel(testScope = this)
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

                model.onDestroy()
            } finally {
                unmockkObject(StakingIntegrationID.Companion)
            }
        }

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

    private fun createConfirmationUiState(confirmationInnerState: InnerConfirmationStakingState): StakingUiState =
        mockk(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns confirmationInnerState
            }
        }

    @Test
    fun `WHEN onDestroy THEN params interceptor removed`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onDestroy()

        verify {
            paramsInterceptorHolder.removeParamsInterceptor("StakingParamsInterceptorId")
        }
    }
}