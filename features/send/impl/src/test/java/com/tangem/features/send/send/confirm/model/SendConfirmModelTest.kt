package com.tangem.features.send.send.confirm.model

import android.os.SystemClock
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeExtraInfo
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.send.SendModelTestBase
import com.tangem.features.send.send.ui.state.SendUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class SendConfirmModelTest : SendModelTestBase() {

    @BeforeEach
    fun mockSystemClock() {
        // SystemClock.elapsedRealtime() is read in init/subscription paths; default to a fresh timer.
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0L
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SystemClock::class)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnSendClick {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN onSendClick THEN send fresh fee else trigger check reload`(model: OnSendClickModel) = runTest {
            // Arrange
            every { SystemClock.elapsedRealtime() } returns model.elapsedRealtime
            val sut = createSendConfirmModel(this, confirmParams(normalFeeState()))
            advanceUntilIdle()

            // Act
            sut.onSendClick()
            advanceUntilIdle()

            // Assert
            if (model.expectedSendInitiated) {
                coVerify(exactly = 1) {
                    createTransferTransactionUseCase(
                        any(),
                        any<Fee>(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                    )
                }
                coVerify(exactly = 0) { feeSelectorCheckReloadTrigger.triggerCheckUpdate() }
            } else {
                coVerify(exactly = 0) {
                    createTransferTransactionUseCase(
                        any(),
                        any<Fee>(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                    )
                }
                coVerify(exactly = 1) { feeSelectorCheckReloadTrigger.triggerCheckUpdate() }
            }
        }

        private fun provideTestModels() = listOf(
            // diff = elapsedRealtime - sendIdleTimer(0); < 10s = fresh -> verify & send
            OnSendClickModel(elapsedRealtime = 0L, expectedSendInitiated = true),
            OnSendClickModel(elapsedRealtime = 20_000L, expectedSendInitiated = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CheckFeeResult {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN check reload result emitted THEN send transaction only on success`(model: CheckFeeResultModel) =
            runTest {
                // Arrange
                val resultFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
                every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns resultFlow
                createSendConfirmModel(this, confirmParams(normalFeeState()))
                advanceUntilIdle()

                // Act
                resultFlow.tryEmit(model.checkResult)
                advanceUntilIdle()

                // Assert
                if (model.expectedSendInitiated) {
                    coVerify(exactly = 1) {
                        createTransferTransactionUseCase(
                            any(),
                            any<Fee>(),
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                        )
                    }
                } else {
                    coVerify(exactly = 0) {
                        createTransferTransactionUseCase(
                            any(),
                            any<Fee>(),
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                        )
                    }
                }
            }

        private fun provideTestModels() = listOf(
            CheckFeeResultModel(checkResult = true, expectedSendInitiated = true),
            CheckFeeResultModel(checkResult = false, expectedSendInitiated = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SendTransactionDispatch {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN send THEN use gasless use case only for token-currency fee`(model: DispatchModel) = runTest {
            // Arrange
            val state = if (model.isTokenCurrencyFee) gaslessFeeState() else normalFeeState()
            val resultFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
            every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns resultFlow
            createSendConfirmModel(this, confirmParams(state))
            advanceUntilIdle()

            // Act
            resultFlow.tryEmit(true)
            advanceUntilIdle()

            // Assert
            if (model.isTokenCurrencyFee) {
                coVerify(exactly = 1) { createAndSendGaslessTransactionUseCase(any(), any(), any()) }
                coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any()) }
            } else {
                coVerify(exactly = 0) { createAndSendGaslessTransactionUseCase(any(), any(), any()) }
                coVerify(exactly = 1) { sendTransactionUseCase(any(), any(), any()) }
            }
        }

        private fun provideTestModels() = listOf(
            DispatchModel(isTokenCurrencyFee = true),
            DispatchModel(isTokenCurrencyFee = false),
        )
    }

    @Nested
    inner class VerifyAndSend {

        @Test
        fun `GIVEN successful send WHEN verifyAndSend THEN notify onSendTransaction`() = runTest {
            // Arrange
            val onSendTransaction = mockk<() -> Unit>(relaxed = true)
            val callback =
                mockk<com.tangem.features.send.send.confirm.SendConfirmComponent.ModelCallback>(relaxed = true)
            val resultFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
            every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns resultFlow
            coEvery { sendTransactionUseCase(any(), any(), any()) } returns "txHash".right()
            val params = MutableParamsContainer(
                defaultSendConfirmParams(
                    state = normalFeeState(),
                    cryptoCurrencyStatus = loadedFeeStatus,
                    feeCryptoCurrencyStatus = loadedFeeStatus,
                ).copy(onSendTransaction = onSendTransaction, callback = callback),
            )
            createSendConfirmModel(this, params)
            advanceUntilIdle()

            // Act
            resultFlow.tryEmit(true)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { onSendTransaction.invoke() }
            verify(exactly = 1) { callback.onResult(any()) }
        }

        @Test
        fun `GIVEN transaction creation fails WHEN verifyAndSend THEN show generic error and do NOT send`() = runTest {
            // Arrange
            val resultFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
            every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns resultFlow
            coEvery {
                createTransferTransactionUseCase(any(), any<Fee>(), any(), any(), any(), any(), any())
            } returns IllegalStateException("boom").left()
            createSendConfirmModel(this, confirmParams(normalFeeState()))
            advanceUntilIdle()

            // Act
            resultFlow.tryEmit(true)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { sendConfirmAlertFactory.getGenericErrorState(any(), any()) }
            coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any()) }
        }
    }

    // region fixtures

    private fun confirmParams(state: SendUM) = MutableParamsContainer(
        defaultSendConfirmParams(
            state = state,
            cryptoCurrencyStatus = loadedFeeStatus,
            feeCryptoCurrencyStatus = loadedFeeStatus,
        ),
    )

    /** Populated Content state with a regular (main-currency) fee — drives the normal send path. */
    private fun normalFeeState(): SendUM = contentState(
        fee = realFee(),
        transactionFeeExtended = null,
    )

    /** Populated Content state where the extended fee is a gasless token-currency fee. */
    private fun gaslessFeeState(): SendUM = contentState(
        fee = realFee(),
        transactionFeeExtended = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = tokenFee()),
            feeTokenId = testCryptoCurrency.id,
        ),
    )

    private fun contentState(fee: Fee, transactionFeeExtended: TransactionFeeExtended?): SendUM {
        val amount = mockk<AmountState.Data>(relaxed = true) {
            every { amountTextField.cryptoAmount.value } returns BigDecimal.ONE
            every { reduceAmountBy } returns BigDecimal.ZERO
            every { isIgnoreReduce } returns false
        }
        val destination = mockk<DestinationUM.Content>(relaxed = true) {
            every { addressTextField.actualAddress } returns "destinationAddr"
            every { memoTextField } returns null
            every { wallets } returns persistentListOf()
        }
        val extraInfo = mockk<FeeExtraInfo>(relaxed = true) {
            every { this@mockk.transactionFeeExtended } returns transactionFeeExtended
            every { feeCryptoCurrencyStatus } returns loadedFeeStatus
        }
        val feeSelector = mockk<FeeSelectorUM.Content>(relaxed = true) {
            every { selectedFeeItem } returns FeeItem.Market(fee)
            every { feeNonce } returns FeeNonce.None
            every { feeExtraInfo } returns extraInfo
            every { isPrimaryButtonEnabled } returns true
        }
        return SendUM(
            amountUM = amount,
            destinationUM = destination,
            feeSelectorUM = feeSelector,
            confirmUM = mockk<ConfirmUM.Content>(relaxed = true),
            confirmData = null,
        )
    }

    private val loadedFeeStatus: CryptoCurrencyStatus
        get() = com.tangem.features.send.loadedStatus(testCryptoCurrency)

    // Can't reuse the shared commonFee(): it builds Amount(blockchain) whose value is null, and
    // verifyAndSendTransaction early-returns on `fee.amount.value ?: return` — so the fee needs an explicit value.
    private fun realFee(): Fee = Fee.Common(
        Amount(currencySymbol = "ETH", value = BigDecimal("0.001"), decimals = 18),
    )

    private fun tokenFee(): Fee.Ethereum.TokenCurrency = Fee.Ethereum.TokenCurrency(
        amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.001"), decimals = 18),
        gasLimit = java.math.BigInteger.valueOf(21_000),
        coinPriceInToken = java.math.BigInteger.ONE,
        feeTransferGasLimit = java.math.BigInteger.ONE,
        baseGas = java.math.BigInteger.ONE,
    )

    data class OnSendClickModel(val elapsedRealtime: Long, val expectedSendInitiated: Boolean)

    data class CheckFeeResultModel(val checkResult: Boolean, val expectedSendInitiated: Boolean)

    data class DispatchModel(val isTokenCurrencyFee: Boolean)

    // endregion
}