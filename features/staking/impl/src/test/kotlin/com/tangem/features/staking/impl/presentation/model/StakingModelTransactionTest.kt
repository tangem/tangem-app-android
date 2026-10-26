package com.tangem.features.staking.impl.presentation.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.ethFee
import com.tangem.test.core.ProvideTestModels
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.StakingTransactionVerdict
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.helpers.StakingFeeLoader
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.features.staking.impl.presentation.state.helpers.StakingTransactionSender
import com.tangem.features.staking.impl.presentation.state.transformers.AddStakingErrorTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateInProgressTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateLoadingTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetConfirmationStateResetAssentTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.notifications.AddStakingNotificationsTransformer
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelTransactionTest : StakingModelTestBase() {

    @Test
    fun `GIVEN loaded model WHEN getFee THEN loading state set and feeLoader called`() = runTest {
        // Arrange
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

        // Act
        model.getFee()
        advanceUntilIdle()

        // Assert
        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> {
                    it is SetConfirmationStateLoadingTransformer
                },
            )
        }
        coVerify {
            mockFeeLoader.getFee(any(), any(), any(), any())
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN fee loader reports fee error WHEN getFee THEN error analytics sent and staking error state set`() =
        runTest {
            // Arrange
            stubNotificationUseCases()
            arrangeFeeLoader { _, _, _, onFeeError -> onFeeError(mockk(relaxed = true)) }
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.getFee()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(StakingAnalyticsEvent.TransactionError(errorCode = "GetFeeError"))
            }
            verify(exactly = 1) { stateController.update(AddStakingErrorTransformer) }
            verify(exactly = 1) {
                stateController.update(match<Transformer<StakingUiState>> { it is AddStakingNotificationsTransformer })
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN fee loader reports approval fee WHEN getFee THEN notifications recomputed`() = runTest {
        // Arrange
        stubNotificationUseCases()
        arrangeFeeLoader { _, _, onApprovalFee, _ -> onApprovalFee(mockk(relaxed = true)) }
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.getFee()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            stateController.update(match<Transformer<StakingUiState>> { it is AddStakingNotificationsTransformer })
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN fee loader reports staking fee WHEN getFee THEN notifications recomputed before and after validation`() =
        runTest {
            // Arrange
            stubNotificationUseCases()
            arrangeFeeLoader { onStakingFee, _, _, _ -> onStakingFee(mockk(relaxed = true), false) }
            arrangeTransactionSender()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.getFee()
            advanceUntilIdle()

            // Assert
            verify(exactly = 2) {
                stateController.update(match<Transformer<StakingUiState>> { it is AddStakingNotificationsTransformer })
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN transaction construction fails WHEN onActionClick THEN error alert sent and assent reset`() = runTest {
        // Arrange
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        every { messageSender.send(any()) } just Runs
        arrangeFeeLoader { _, _, _, _ -> }
        arrangeTransactionSender { callbacks -> callbacks.onConstructError(mockk(relaxed = true)) }
        val model = createModel(testScope = this)
        advanceUntilIdle()
        uiStateFlow.value = assentUiState()
        advanceUntilIdle()
        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)

        // Act
        model.onActionClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(match { it is DialogMessage }) }
        verify(exactly = 1) {
            stateController.update(
                match<Transformer<StakingUiState>> { it is SetConfirmationStateResetAssentTransformer },
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN P2PEthPool fee increased WHEN fee update confirmed THEN confirmation set in progress again and sent`() =
        runTest {
            // Arrange
            coEvery { p2pEthPoolRepository.getVaultsSync() } returns emptyList()
            coEvery { p2pEthPoolRepository.getVaultLimitsSyncOrNull() } returns emptyMap()
            val uiStateFlow = MutableStateFlow(initialUiState)
            every { stateController.uiState } returns uiStateFlow
            stubNotificationUseCases()
            val messages = mutableListOf<Any>()
            every { messageSender.send(any()) } answers { messages += firstArg<Any>() }
            val newFee = mockk<Fee.Common>(relaxed = true) { every { amount.value } returns BigDecimal.TEN }
            val currentFee = mockk<Fee.Common>(relaxed = true) { every { amount.value } returns BigDecimal.ONE }
            arrangeFeeLoader { onStakingFee, _, _, _ -> onStakingFee(newFee, false) }
            val transactionSender = arrangeTransactionSender()
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
            val assentState = assentUiState(currentFee)
            uiStateFlow.value = assentState
            every { stateController.value } returns assentState
            advanceUntilIdle()
            model.onActionClick()
            advanceUntilIdle()

            // Act
            messages.filterIsInstance<DialogMessage>().single().firstAction.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 2) {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is SetConfirmationStateInProgressTransformer },
                )
            }
            coVerify(exactly = 1) { transactionSender.send(any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN P2PEthPool fee not loaded yet WHEN onActionClick THEN fee updated alert shown`() = runTest {
        // Arrange
        coEvery { p2pEthPoolRepository.getVaultsSync() } returns emptyList()
        coEvery { p2pEthPoolRepository.getVaultLimitsSyncOrNull() } returns emptyMap()
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        stubNotificationUseCases()
        every { messageSender.send(any()) } just Runs
        arrangeFeeLoader { onStakingFee, _, _, _ ->
            onStakingFee(mockk<Fee.Common>(relaxed = true) { every { amount.value } returns BigDecimal.ONE }, false)
        }
        val transactionSender = arrangeTransactionSender()
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
        val assentState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns InnerConfirmationStakingState.ASSENT
                every { feeState } returns FeeState.Loading
            }
        }
        uiStateFlow.value = assentState
        every { stateController.value } returns assentState
        advanceUntilIdle()

        // Act
        model.onActionClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(match { it is DialogMessage }) }
        coVerify(exactly = 0) { transactionSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN transaction sent successfully WHEN onFailedTxEmailClick THEN email has no transactions in progress`() =
        runTest {
            // Arrange
            val uiStateFlow = MutableStateFlow(initialUiState)
            every { stateController.uiState } returns uiStateFlow
            every { stateController.value } returns initialUiState
            arrangeFeeLoader { _, _, _, _ -> }
            val constructed = mockk<StakingTransaction> {
                every { type } returns StakingTransactionType.STAKE
                every { unsignedTransaction } returns "unsigned"
            }
            arrangeTransactionSender { callbacks ->
                callbacks.onConstructSuccess(listOf(constructed))
                callbacks.onSendSuccess("https://explorer/tx")
            }
            val walletMetaInfo: WalletMetaInfo = mockk(relaxed = true)
            coEvery { getWalletMetaInfoUseCase(userWalletId = any()) } returns Either.Right(walletMetaInfo)
            every { saveBlockchainErrorUseCase(error = any()) } just Runs
            coEvery { sendFeedbackEmailUseCase(type = any()) } just Runs
            val model = createModel(testScope = this)
            advanceUntilIdle()
            uiStateFlow.value = assentUiState()
            advanceUntilIdle()
            model.onActionClick()
            advanceUntilIdle()

            // Act
            model.onFailedTxEmailClick("error")
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                sendFeedbackEmailUseCase(
                    match { it is FeedbackEmailType.StakingProblem && it.transactionTypes.isEmpty() },
                )
            }

            model.onDestroy()
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN confirmation inputs WHEN notifications recomputed THEN fee checks get amount fee and balance after`(
        model: NotificationInputsModel,
    ) = runTest {
        // Arrange
        stubNotificationUseCases()
        val status = currencyStatus(amount = BigDecimal.TEN)
        val accountStatus = accountCurrencyStatusOf(status)
        every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns flowOf(accountStatus)
        val feeStatus = status.takeIf { model.hasFeeStatus }
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, status) } returns Either.Right(feeStatus)
        coEvery {
            isAmountSubtractAvailableUseCase(testUserWalletId, any())
        } returns Either.Right(model.isSubtractAvailable)
        every { stateController.uiState } returns MutableStateFlow(
            stakingUiState(
                actionType = model.actionType,
                amountState = model.amountState,
                confirmationState = model.confirmationState,
            ),
        )
        arrangeFeeLoader { _, _, onApprovalFee, _ -> onApprovalFee(mockk(relaxed = true)) }
        val stakingModel = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        stakingModel.getFee()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            getCurrencyCheckUseCase(
                userWalletId = testUserWalletId,
                currencyStatus = status,
                feeCurrencyStatus = feeStatus,
                amount = model.expectedAmount?.let { expected -> match { it?.compareTo(expected) == 0 } } ?: isNull(),
                fee = model.expectedFee?.let { expected -> match { it?.compareTo(expected) == 0 } } ?: isNull(),
                feeCurrencyBalanceAfterTransaction = match { it?.compareTo(model.expectedBalanceAfter) == 0 },
                recipientAddress = any(),
            )
        }
        coVerify(exactly = if (model.isFeeWarningChecked) 1 else 0) {
            getBalanceNotEnoughForFeeWarningUseCase(
                fee = any(),
                userWalletId = testUserWalletId,
                tokenStatus = status,
                feeStatus = any(),
            )
        }
        verify(exactly = 1) {
            stateController.update(match<Transformer<StakingUiState>> { it is AddStakingNotificationsTransformer })
        }

        stakingModel.onDestroy()
    }

    data class NotificationInputsModel(
        val actionType: StakingActionCommonType,
        val amountState: AmountState,
        val confirmationState: StakingStates.ConfirmationState,
        val hasFeeStatus: Boolean,
        val isSubtractAvailable: Boolean,
        val expectedAmount: BigDecimal?,
        val expectedFee: BigDecimal?,
        val expectedBalanceAfter: BigDecimal,
        val isFeeWarningChecked: Boolean,
    )

    @ParameterizedTest
    @MethodSource("provideRentWarningModels")
    fun `GIVEN fee error WHEN rent warning present or absent THEN fee unreachable notification hidden or shown`(
        model: RentWarningModel,
    ) = runTest {
        // Arrange
        coEvery { getBalanceNotEnoughForFeeWarningUseCase(any(), any(), any(), any()) } returns Either.Right(null)
        coEvery { getCurrencyCheckUseCase(any(), any(), any(), any(), any(), any(), any()) } returns
            CryptoCurrencyCheck(
                dustValue = null,
                reserveAmount = null,
                minimumSendAmount = null,
                existentialDeposit = null,
                utxoAmountLimit = null,
                isAccountFunded = true,
                rentWarning = if (model.hasRentWarning) mockk(relaxed = true) else null,
            )
        arrangeFeeLoader { _, _, _, onFeeError -> onFeeError(GetFeeError.UnknownError) }
        val stakingModel = createModel(testScope = this)
        advanceUntilIdle()
        val transformers = mutableListOf<Transformer<StakingUiState>>()
        every { stateController.update(capture(transformers)) } just Runs

        // Act
        stakingModel.getFee()
        advanceUntilIdle()

        // Assert
        val confirmation = transformers.filterIsInstance<AddStakingNotificationsTransformer>().single()
            .transform(createAssentConfirmationUiState()).confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.notifications.any { it is NotificationUM.Warning.NetworkFeeUnreachable })
            .isEqualTo(!model.hasRentWarning)

        stakingModel.onDestroy()
    }

    data class RentWarningModel(val hasRentWarning: Boolean)

    companion object {
        private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)
        private val exit = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)

        @JvmStatic
        fun provideTestModels() = listOf(
            NotificationInputsModel(
                actionType = enter,
                amountState = amountData(cryptoAmount = BigDecimal("9.8")),
                confirmationState = confirmationData(
                    feeState = feeContent(ethFee(BigDecimal("0.01"))),
                    reduceAmountBy = BigDecimal("0.2"),
                ),
                hasFeeStatus = true,
                isSubtractAvailable = true,
                expectedAmount = BigDecimal("9.8"),
                expectedFee = BigDecimal("0.01"),
                expectedBalanceAfter = BigDecimal("0.2"),
                isFeeWarningChecked = true,
            ),
            NotificationInputsModel(
                actionType = exit,
                amountState = AmountState.Empty,
                confirmationState = StakingStates.ConfirmationState.Empty(),
                hasFeeStatus = true,
                isSubtractAvailable = false,
                expectedAmount = null,
                expectedFee = null,
                expectedBalanceAfter = BigDecimal.TEN,
                isFeeWarningChecked = false,
            ),
            NotificationInputsModel(
                actionType = exit,
                amountState = amountData(cryptoAmount = BigDecimal("1.5")),
                confirmationState = confirmationData(feeState = feeContent(ethFee(BigDecimal("0.01")))),
                hasFeeStatus = true,
                isSubtractAvailable = false,
                expectedAmount = BigDecimal("1.5"),
                expectedFee = BigDecimal("0.01"),
                expectedBalanceAfter = BigDecimal("9.99"),
                isFeeWarningChecked = true,
            ),
            NotificationInputsModel(
                actionType = enter,
                amountState = amountData(cryptoAmount = BigDecimal("1.5")),
                confirmationState = confirmationData(feeState = feeContent(ethFee(BigDecimal("0.01")))),
                hasFeeStatus = false,
                isSubtractAvailable = false,
                expectedAmount = BigDecimal("1.5"),
                expectedFee = BigDecimal("0.01"),
                expectedBalanceAfter = BigDecimal("8.49"),
                isFeeWarningChecked = false,
            ),
        )

        @JvmStatic
        fun provideRentWarningModels() = listOf(
            RentWarningModel(hasRentWarning = false),
            RentWarningModel(hasRentWarning = true),
        )
    }

    @Test
    fun `GIVEN no TON activation transaction prepared WHEN onActivateTonAccountClick THEN nothing sent`() = runTest {
        // Arrange
        coEvery { sendTransactionUseCase(any(), any(), any()) } returns Either.Left(mockk(relaxed = true))
        every { messageSender.send(any()) } just Runs
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onActivateTonAccountClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any()) }
        verify(exactly = 0) { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN no network address WHEN onActivateTonAccountNotificationClick THEN no activation transaction built`() =
        runTest {
            // Arrange
            coEvery { getNetworkAddressesUseCase.invokeSync(userWalletId = any(), network = any<Network>()) } returns
                emptyList()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onActivateTonAccountNotificationClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { createTransferTransactionUseCase(any(), any(), any(), any(), any()) }

            model.onDestroy()
        }

    private fun arrangeFeeLoader(
        onGetFee: (
            onStakingFee: (Fee, Boolean) -> Unit,
            onStakingFeeError: (StakingError) -> Unit,
            onApprovalFee: (TransactionFee) -> Unit,
            onFeeError: (GetFeeError) -> Unit,
        ) -> Unit,
    ) {
        val feeLoader = mockk<StakingFeeLoader> {
            coEvery { getFee(any(), any(), any(), any()) } answers {
                onGetFee(firstArg(), secondArg(), thirdArg(), arg(3))
            }
        }
        every { stakingOperationsFactory.createFeeLoader(any(), any(), any()) } returns feeLoader
    }

    private fun arrangeTransactionSender(
        onSend: (StakingTransactionSender.Callbacks) -> Unit = {},
    ): StakingTransactionSender {
        val transactionSender = mockk<StakingTransactionSender> {
            coEvery { validate() } returns StakingTransactionVerdict.SAFE
            coEvery { send(any()) } answers { onSend(firstArg()) }
        }
        every {
            stakingOperationsFactory.createTransactionSender(
                cryptoCurrencyStatusProvider = any(),
                userWallet = any(),
                integration = any(),
                isAmountSubtractAvailable = any(),
            )
        } returns transactionSender
        return transactionSender
    }

    private fun assentUiState(currentFee: Fee? = null): StakingUiState = mockk(relaxed = true) {
        every { currentStep } returns StakingStep.Confirmation
        every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
            every { innerState } returns InnerConfirmationStakingState.ASSENT
            every { feeState } returns mockk<FeeState.Content>(relaxed = true) {
                every { fee } returns currentFee
            }
        }
    }

    @Test
    fun `GIVEN StakeKit integration AND assent state WHEN onActionClick THEN sendTransaction called`() = runTest {
        // Arrange
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
            coEvery { validate() } returns StakingTransactionVerdict.SAFE
            coEvery { send(any()) } just Runs
        }
        every {
            stakingOperationsFactory.createTransactionSender(
                cryptoCurrencyStatusProvider = any(),
                userWallet = any(),
                integration = any(),
                isAmountSubtractAvailable = any(),
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

        // Act
        model.onActionClick()
        advanceUntilIdle()

        // Assert
        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> {
                    it is SetConfirmationStateInProgressTransformer
                },
            )
        }
        coVerify { mockTransactionSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN P2PEthPool AND fee not increased WHEN onActionClick THEN sendTransaction called directly`() = runTest {
        // Arrange
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
            coEvery { validate() } returns StakingTransactionVerdict.SAFE
            coEvery { send(any()) } just Runs
        }
        every {
            stakingOperationsFactory.createTransactionSender(
                cryptoCurrencyStatusProvider = any(),
                userWallet = any(),
                integration = any(),
                isAmountSubtractAvailable = any(),
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

        // Act
        model.onActionClick()
        advanceUntilIdle()

        // Assert
        coVerify { mockTransactionSender.send(any()) }
        verify(exactly = 0) { messageSender.send(match { it is DialogMessage }) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN P2PEthPool AND fee increased WHEN onActionClick THEN fee updated alert shown`() = runTest {
        // Arrange
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
                feeStatus = any(),
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
                any(),
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
            coEvery { validate() } returns StakingTransactionVerdict.SAFE
            coEvery { send(any()) } just Runs
        }
        every {
            stakingOperationsFactory.createTransactionSender(
                cryptoCurrencyStatusProvider = any(),
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

        // Act
        model.onActionClick()
        advanceUntilIdle()

        // Assert
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
    fun `GIVEN approval needed AND amountState data WHEN getApprovalParams THEN returns non-null params`() = runTest {
        // Arrange
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

            // Act
            val result = model.getApprovalParams()

            // Assert
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
            // Arrange
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
                    network = any<Network>(),
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

            // Act
            model.onActivateTonAccountNotificationClick()
            advanceUntilIdle()

            // Assert
            verify {
                analyticsEventHandler.send(StakingAnalyticsEvent.UninitializedAddressScreen("KEK"))
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ShowTonInitializeBottomSheetTransformer },
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
            // Arrange
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
                    network = any<Network>(),
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

            // Act
            model.onActivateTonAccountNotificationClick()
            advanceUntilIdle()

            // Assert
            verify {
                analyticsEventHandler.send(StakingAnalyticsEvent.UninitializedAddressScreen("SHMEK"))
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ShowTonInitializeBottomSheetTransformer },
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
    fun `GIVEN TON currency WHEN onActivateTonAccountNotificationShow THEN UninitializedAddress analytics sent with token`() =
        runTest {
            // Arrange
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

            // Act
            model.onActivateTonAccountNotificationShow()

            // Assert
            verify {
                analyticsEventHandler.send(StakingAnalyticsEvent.UninitializedAddress(token = "TON"))
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN SOL currency with zero balance WHEN onNotEnoughFeeNotificationShow THEN NotEnoughFee analytics sent with token`() =
        runTest {
            // Arrange
            val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
            every { testCryptoCurrencyStatus.currency.symbol } returns "SOL"
            every { testCryptoCurrencyStatus.currency.network.name } returns "solana"
            every { testCryptoCurrencyStatus.value.amount } returns BigDecimal.ZERO
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

            // Act
            model.onNotEnoughFeeNotificationShow()

            // Assert
            verify {
                analyticsEventHandler.send(
                    StakingAnalyticsEvent.NotEnoughFee(
                        token = "SOL",
                        blockchain = "solana",
                        balance = AnalyticsParam.TokenBalanceState.Empty,
                    ),
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN sendTransaction returns Left WHEN onActivateTonAccountClick THEN error dialog sent`() = runTest {
        // Arrange
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
                network = any<Network>(),
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

        // Act
        model.onActivateTonAccountClick()
        advanceUntilIdle()

        // Assert
        verify {
            analyticsEventHandler.send(StakingAnalyticsEvent.ButtonActivate(token = "TON"))
        }
        verify { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN sendTransaction returns Right WHEN onActivateTonAccountClick THEN complete transformer applied`() =
        runTest {
            // Arrange
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
            coEvery { mockBalanceUpdater.partialUpdateWithDelay() } just Runs
            coEvery {
                getNetworkAddressesUseCase.invokeSync(
                    userWalletId = any(),
                    network = any<Network>(),
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

            // Act
            model.onActivateTonAccountClick()
            advanceUntilIdle()

            // Assert
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