package com.tangem.features.staking.impl.presentation.model

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.helpers.StakingBalanceUpdater
import com.tangem.features.staking.impl.presentation.state.helpers.StakingFeeLoader
import com.tangem.features.staking.impl.presentation.state.helpers.StakingTransactionSender
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateInProgressTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateLoadingTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateResetAssentTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetApprovalBottomSheetInProgressTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetApprovalBottomSheetTypeChangeTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.ShowApprovalBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.CompleteInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.SetFeeErrorToTonInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.SetFeeToTonInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.ShowTonInitializeBottomSheetTransformer
import com.tangem.utils.transformer.Transformer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelTransactionTest : StakingModelTestBase() {

    @Test
    fun `WHEN getFee THEN loading state set and feeLoader called`() = runTest {
        val mockFeeLoader = mockk<StakingFeeLoader> {
            coEvery {
                getFee(any(), any(), any(), any())
            } just Runs
        }
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockFeeLoader

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.getFee()
        advanceUntilIdle()

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> {
                    it is SetConfirmationStateLoadingTransformer
                }
            )
        }
        coVerify {
            mockFeeLoader.getFee(any(), any(), any(), any())
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN StakeKit integration AND assent state WHEN onActionClick THEN sendTransaction called`() = runTest {
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        val mockFeeLoader = mockk<StakingFeeLoader> {
            coEvery {
                getFee(any(), any(), any(), any())
            } just Runs
        }
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockFeeLoader
        val mockTransactionSender = mockk<StakingTransactionSender> {
            coEvery { send(any()) } just Runs
        }
        every {
            stakingOperationsFactory.createTransactionSender(
                cryptoCurrencyStatus = any(),
                userWallet = any(),
                integration = any(),
                isAmountSubtractAvailable = any()
            )
        } returns mockTransactionSender

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val assentUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data> {
                every { innerState } returns InnerConfirmationStakingState.ASSENT
            }
        }
        uiStateFlow.value = assentUiState
        every { stateController.value } returns assentUiState
        advanceUntilIdle()

        model.onActionClick()
        advanceUntilIdle()

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> {
                    it is SetConfirmationStateInProgressTransformer
                }
            )
        }
        coVerify { mockTransactionSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN P2PEthPool AND fee not increased WHEN onActionClick THEN sendTransaction called directly`() = runTest {
        val p2pParams = StakingComponent.Params(
            userWalletId = testUserWalletId,
            cryptoCurrency = testCryptoCurrency,
            integrationId = StakingIntegrationID.P2PEthPool,
        )
        coEvery { p2pEthPoolRepository.getVaultsSync() } returns emptyList()
        coEvery { p2pEthPoolRepository.getVaultLimitsSyncOrNull() } returns emptyMap()
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        coEvery {
            getBalanceNotEnoughForFeeWarningUseCase(
                fee = any(),
                userWalletId = any(),
                tokenStatus = any(),
                feeStatus = any()
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
                recipientAddress = any()
            )
        } returns mockk(relaxed = true)
        val newFee = mockk<Fee.Common>(relaxed = true) {
            every { amount.value } returns BigDecimal.ONE
        }
        val mockFeeLoader = mockk<StakingFeeLoader> {
            coEvery {
                getFee(any(), any(), any(), any())
            } coAnswers {
                firstArg<(Fee, Boolean) -> Unit>().invoke(newFee, false)
            }
        }
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockFeeLoader
        val mockTransactionSender = mockk<StakingTransactionSender> {
            coEvery { send(any()) } just Runs
        }
        every {
            stakingOperationsFactory.createTransactionSender(
                cryptoCurrencyStatus = any(),
                userWallet = any(),
                integration = any(),
                isAmountSubtractAvailable = any()
            )
        } returns mockTransactionSender
        val currentFee = mockk<Fee.Common>(relaxed = true) {
            every { amount.value } returns BigDecimal.TEN
        }
        val assentUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns InnerConfirmationStakingState.ASSENT
                every { feeState } returns mockk<FeeState.Content>(relaxed = true) {
                    every { fee } returns currentFee
                }
            }
        }

        val model = createModel(
            paramsContainer = MutableParamsContainer(p2pParams),
            testScope = this,
        )
        advanceUntilIdle()

        uiStateFlow.value = assentUiState
        every { stateController.value } returns assentUiState
        advanceUntilIdle()

        model.onActionClick()
        advanceUntilIdle()

        coVerify { mockTransactionSender.send(any()) }
        verify(exactly = 0) { messageSender.send(match { it is DialogMessage }) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN P2PEthPool AND fee increased WHEN onActionClick THEN fee updated alert shown`() = runTest {
        val p2pParams = StakingComponent.Params(
            userWalletId = testUserWalletId,
            cryptoCurrency = testCryptoCurrency,
            integrationId = StakingIntegrationID.P2PEthPool,
        )
        coEvery { p2pEthPoolRepository.getVaultsSync() } returns emptyList()
        coEvery { p2pEthPoolRepository.getVaultLimitsSyncOrNull() } returns emptyMap()
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        coEvery {
            getBalanceNotEnoughForFeeWarningUseCase(
                fee = any(),
                userWalletId = any(),
                tokenStatus = any(),
                feeStatus = any()
            )
        } returns Either.Right(null)
        coEvery {
            getCurrencyCheckUseCase(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockk(relaxed = true)
        every { messageSender.send(any()) } just Runs
        val newFee = mockk<Fee.Common>(relaxed = true) {
            every { amount.value } returns BigDecimal.TEN
        }
        val mockFeeLoader = mockk<StakingFeeLoader> {
            coEvery {
                getFee(any(), any(), any(), any())
            } coAnswers {
                firstArg<(Fee, Boolean) -> Unit>().invoke(newFee, false)
            }
        }
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockFeeLoader
        val mockTransactionSender = mockk<StakingTransactionSender> {
            coEvery { send(any()) } just Runs
        }
        every {
            stakingOperationsFactory.createTransactionSender(
                cryptoCurrencyStatus = any(),
                userWallet = any(),
                integration = any(),
                isAmountSubtractAvailable = any(),
            )
        } returns mockTransactionSender
        val currentFee = mockk<Fee.Common>(relaxed = true) {
            every { amount.value } returns BigDecimal.ONE
        }
        val assentUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns InnerConfirmationStakingState.ASSENT
                every { feeState } returns mockk<FeeState.Content>(relaxed = true) {
                    every { fee } returns currentFee
                }
            }
        }

        val model = createModel(
            paramsContainer = MutableParamsContainer(p2pParams),
            testScope = this,
        )
        advanceUntilIdle()

        uiStateFlow.value = assentUiState
        every { stateController.value } returns assentUiState
        advanceUntilIdle()

        model.onActionClick()
        advanceUntilIdle()

        verify {
            stateController.update(
                match<Transformer<StakingUiState>> {
                    it is SetConfirmationStateResetAssentTransformer
                },
            )
        }
        verify { messageSender.send(any()) }
        coVerify(exactly = 0) { mockTransactionSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN gasless approval enabled WHEN showApprovalBottomSheet THEN approvalSlotNavigation activated`() =
        runTest {
            every { giveApprovalFeatureToggles.isGaslessApprovalEnabled } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.showApprovalBottomSheet()

            verify(exactly = 0) {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ShowApprovalBottomSheetTransformer }
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN gasless disabled WHEN showApprovalBottomSheet THEN ShowApprovalBottomSheetTransformer applied`() =
        runTest {
            every { giveApprovalFeatureToggles.isGaslessApprovalEnabled } returns false

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.showApprovalBottomSheet()

            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ShowApprovalBottomSheetTransformer }
                )
            }

            model.onDestroy()
        }

    @Test
    fun `WHEN onApproveTypeChange THEN SetApprovalBottomSheetTypeChangeTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onApproveTypeChange(ApproveType.LIMITED)

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is SetApprovalBottomSheetTypeChangeTransformer },
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN approval needed WHEN onApprovalClick THEN in progress set and createApprovalTransaction called`() =
        runTest {
            val spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"
            val expectedNetwork = mockk<Network> {
                every { name } returns "KEK"
            }
            val testToken: CryptoCurrency.Token = mockk(relaxed = true) {
                every { network } returns expectedNetwork
            }
            val testCryptoCurrencyStatus: CryptoCurrencyStatus = mockk(relaxed = true) {
                every { currency } returns testToken
            }
            val testAccountCurrencyStatus = mockk<AccountCryptoCurrencyStatus> {
                every { component1() } returns mockk(relaxed = true)
                every { component2() } returns testCryptoCurrencyStatus
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

            // Setup stakingApproval = Needed
            mockkObject(StakingIntegrationID.Companion)
            every {
                StakingIntegrationID.create(any())
            } returns mockk {
                every { approval } returns StakingApproval.Needed(spenderAddress)
            }
            coEvery {
                getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
            } returns Either.Right(BigDecimal.TEN)

            every {
                stakingOperationsFactory.createFeeLoader(
                    cryptoCurrencyStatus = any(),
                    userWallet = any(),
                    integration = any()
                )
            } returns mockk<StakingFeeLoader> {
                coEvery {
                    getFee(
                        onStakingFee = any(),
                        onStakingFeeError = any(),
                        onApprovalFee = any(),
                        onFeeError = any()
                    )
                } just Runs
            }
            val expectedApprovalTx = Either.Right<TransactionData.Uncompiled>(mockk(relaxed = true))
            coEvery {
                createApprovalTransactionUseCase.invoke(
                    cryptoCurrencyStatus = any(),
                    userWalletId = any(),
                    amount = any(),
                    fee = any(),
                    contractAddress = any(),
                    spenderAddress = any(),
                )
            } returns expectedApprovalTx
            coEvery {
                sendTransactionUseCase(any(), any(), any())
            } returns Either.Right("txHash")
            every { vibratorHapticManager.performOneTime(any()) } just Runs

            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Now override stateController.value with confirmation state after cryptoCurrencyStatus is initialized
            val testFee: Fee.Common = mockk(relaxed = true)
            val confirmationState = mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { feeState } returns mockk<FeeState.Content>(relaxed = true) {
                    every { fee } returns testFee
                }
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.confirmationState } returns confirmationState
                every { bottomSheetConfig } returns null
            }
            every { stateController.value } returns uiState

            model.onApprovalClick()
            advanceUntilIdle()

            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> {
                        it is SetApprovalBottomSheetInProgressTransformer
                    },
                )
            }
            coVerify {
                sendTransactionUseCase(
                    txData = expectedApprovalTx.value,
                    userWallet = testUserWallet,
                    network = expectedNetwork,
                )
            }

            model.onDestroy()
            unmockkObject(StakingIntegrationID.Companion)
        }

    @Test
    fun `GIVEN approval needed AND amountState data WHEN getApprovalParams THEN returns non-null params`() = runTest {
        val spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"

        mockkObject(StakingIntegrationID.Companion)
        try {
            every {
                StakingIntegrationID.create(any())
            } returns mockk {
                every { approval } returns StakingApproval.Needed(spenderAddress)
            }
            coEvery {
                getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
            } returns Either.Right(BigDecimal.TEN)

            val amountState = mockk<AmountState.Data>(relaxed = true) {
                every { amountTextField.value } returns "100"
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.amountState } returns amountState
            }
            every { stateController.value } returns uiState

            val model = createModel(testScope = this)
            advanceUntilIdle()

            val result = model.getApprovalParams()

            assert(result != null) { "Expected non-null GiveApprovalComponent.Params" }
            assert(result!!.spenderAddress == spenderAddress) {
                "Expected spenderAddress=$spenderAddress, got=${result.spenderAddress}"
            }

            model.onDestroy()
        } finally {
            unmockkObject(StakingIntegrationID.Companion)
        }
    }

    @Test
    fun `GIVEN getFee returns Left WHEN onActivateTonAccountNotificationClick THEN fee error transformer applied`() =
        runTest {
            val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
            every { testCryptoCurrencyStatus.currency } returns mockk {
                every { id } returns CryptoCurrency.ID.fromValue("coin⟨ethereum→-1843072795⟩ethereum")
                every { symbol } returns "KEK"
                every { network } returns mockk()
                every { decimals } returns 2
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
            coEvery {
                getNetworkAddressesUseCase.invokeSync(
                    userWalletId = any(),
                    network = any<Network>()
                )
            } returns listOf(mockk(relaxed = true) { every { address } returns "TON_ADDRESS" })
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(), memo = any(), destination = any(),
                    userWalletId = any(), network = any(),
                )
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns Either.Left(mockk(relaxed = true))

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onActivateTonAccountNotificationClick()
            advanceUntilIdle()

            verify {
                analyticsEventHandler.send(StakingAnalyticsEvent.UninitializedAddressScreen("KEK"))
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ShowTonInitializeBottomSheetTransformer }
                )
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> {
                        it is SetFeeErrorToTonInitializeBottomSheetTransformer
                    },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN getFee returns Right WHEN onActivateTonAccountNotificationClick THEN fee transformer applied`() =
        runTest {
            val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
            every { testCryptoCurrencyStatus.currency } returns mockk {
                every { id } returns CryptoCurrency.ID.fromValue("coin⟨ethereum→-1843072795⟩ethereum")
                every { symbol } returns "SHMEK"
                every { network } returns mockk()
                every { decimals } returns 2
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
            coEvery {
                getNetworkAddressesUseCase.invokeSync(
                    userWalletId = any(),
                    network = any<Network>()
                )
            } returns listOf(mockk(relaxed = true) { every { address } returns "TON_ADDRESS" })
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(), memo = any(), destination = any(),
                    userWalletId = any(), network = any(),
                )
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns Either.Right(mockk(relaxed = true))

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onActivateTonAccountNotificationClick()
            advanceUntilIdle()

            verify {
                analyticsEventHandler.send(StakingAnalyticsEvent.UninitializedAddressScreen("SHMEK"))
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ShowTonInitializeBottomSheetTransformer }
                )
            }
            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> {
                        it is SetFeeToTonInitializeBottomSheetTransformer
                    },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `WHEN onActivateTonAccountNotificationShow THEN UninitializedAddress analytics sent with token`() = runTest {
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every { testCryptoCurrencyStatus.currency.symbol } returns "TON"
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

        model.onActivateTonAccountNotificationShow()

        verify {
            analyticsEventHandler.send(StakingAnalyticsEvent.UninitializedAddress(token = "TON"))
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onNotEnoughFeeNotificationShow THEN NotEnoughFee analytics sent with token`() = runTest {
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every { testCryptoCurrencyStatus.currency.symbol } returns "SOL"
        every { testCryptoCurrencyStatus.currency.network.name } returns "solana"
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

        model.onNotEnoughFeeNotificationShow()

        verify {
            analyticsEventHandler.send(
                StakingAnalyticsEvent.NotEnoughFee(
                    token = "SOL",
                    blockchain = "solana",
                )
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN sendTransaction returns Left WHEN onActivateTonAccountClick THEN error dialog sent`() = runTest {
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every { testCryptoCurrencyStatus.currency.symbol } returns "TON"
        every {
            getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
        } returns flowOf(testAccountCurrencyStatus)
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Left(mockk())
        coEvery {
            getNetworkAddressesUseCase.invokeSync(
                userWalletId = any(),
                network = any<Network>()
            )
        } returns listOf(mockk(relaxed = true) { every { address } returns "TON_ADDRESS" })
        coEvery {
            createTransferTransactionUseCase(
                amount = any(), memo = any(), destination = any(),
                userWalletId = any(), network = any(),
            )
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            sendTransactionUseCase(any(), any(), any())
        } returns Either.Left(mockk(relaxed = true))
        every { messageSender.send(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        // First populate tonAccountInitializeTransaction
        model.onActivateTonAccountNotificationClick()
        advanceUntilIdle()

        model.onActivateTonAccountClick()
        advanceUntilIdle()

        verify {
            analyticsEventHandler.send(StakingAnalyticsEvent.ButtonActivate(token = "TON"))
        }
        verify { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN sendTransaction returns Right WHEN onActivateTonAccountClick THEN complete transformer applied`() =
        runTest {
            val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
            every { testCryptoCurrencyStatus.currency.symbol } returns "TON"
            every {
                getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
            } returns flowOf(testAccountCurrencyStatus)
            coEvery {
                getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
            } returns Either.Left(mockk())
            val mockBalanceUpdater: StakingBalanceUpdater = mockk {
                coEvery { partialUpdate() } just Runs
                coEvery { partialUpdateWithDelay() } just Runs
            }
            every {
                stakingBalanceUpdater.create(any(), any(), any())
            } returns mockBalanceUpdater
            coEvery {
                getNetworkAddressesUseCase.invokeSync(
                    userWalletId = any(),
                    network = any<Network>()
                )
            } returns listOf(mockk(relaxed = true) { every { address } returns "TON_ADDRESS" })
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(), memo = any(), destination = any(),
                    userWalletId = any(), network = any(),
                )
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                sendTransactionUseCase(any(), any(), any())
            } returns Either.Right("txHash")

            val model = createModel(testScope = this)
            advanceUntilIdle()

            // First populate tonAccountInitializeTransaction
            model.onActivateTonAccountNotificationClick()
            advanceUntilIdle()

            model.onActivateTonAccountClick()
            advanceUntilIdle()

            verify {
                analyticsEventHandler.send(StakingAnalyticsEvent.ButtonActivate(token = "TON"))
            }
            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is CompleteInitializeBottomSheetTransformer },
                )
            }
            coVerify { mockBalanceUpdater.partialUpdateWithDelay() }

            model.onDestroy()
        }
}