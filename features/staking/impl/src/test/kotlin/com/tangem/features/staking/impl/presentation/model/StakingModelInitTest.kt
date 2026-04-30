package com.tangem.features.staking.impl.presentation.model

import arrow.core.Either
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.helpers.StakingFeeLoader
import com.tangem.features.staking.impl.presentation.state.transformers.HideBalanceStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateLoadingTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetInitialDataStateTransformer
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.Transformer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelInitTest : StakingModelTestBase() {

    @Test
    fun `GIVEN currency status emitted WHEN model created THEN analytics sent and fee status fetched`() = runTest {
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every { testCryptoCurrencyStatus.value } returns mockk {
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
                    cryptoCurrencyStatus = any(),
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
    fun `WHEN onDestroy THEN params interceptor removed`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onDestroy()

        verify {
            paramsInterceptorHolder.removeParamsInterceptor("StakingParamsInterceptorId")
        }
    }
}