package com.tangem.features.send.send.confirm.model

import android.os.SystemClock
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationRecipientListUM
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
import kotlinx.collections.immutable.ImmutableList
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
                        any(),
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
                        any(),
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
                            any(),
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
                            any(),
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

    @Nested
    inner class TronGaslessMaxSend {

        @BeforeEach
        fun stubTronGaslessSend() {
            // The shared base leaves this one relaxed, which yields an Either that blows up on fold().
            coEvery {
                createAndSendTronGaslessTransactionUseCase(any(), any(), any(), any())
            } returns "txHash".right()
        }

        @Test
        fun `GIVEN max amount and Tron gasless fee in the sent token WHEN send THEN amount reduced by the fee`() =
            runTest {
                // Arrange — the whole balance is entered and the compensation is paid in that same token.
                coEvery { isAmountSubtractAvailableUseCase(any(), any(), any()) } returns true.right()
                val amountSlot = slot<Amount>()
                coEvery {
                    createTransferTransactionUseCase(
                        capture(amountSlot),
                        any<Fee>(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns mockk<TransactionData.Uncompiled>(relaxed = true).right()
                val sut = createSendConfirmModel(this, tronGaslessParams())
                advanceUntilIdle()

                // Act
                sut.onSendClick()
                advanceUntilIdle()

                // Assert
                assertThat(amountSlot.captured.value).isEqualTo(TRON_BALANCE - TRON_GASLESS_FEE)
            }

        @Test
        fun `GIVEN Tron gasless quote WHEN send THEN dispatched through the Tron gasless use case`() = runTest {
            // Arrange
            coEvery { isAmountSubtractAvailableUseCase(any(), any(), any()) } returns true.right()
            val sut = createSendConfirmModel(this, tronGaslessParams())
            advanceUntilIdle()

            // Act
            sut.onSendClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { createAndSendTronGaslessTransactionUseCase(any(), any(), any(), any()) }
            coVerify(exactly = 0) { createAndSendGaslessTransactionUseCase(any(), any(), any()) }
            coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN Tron gasless fee WHEN model created THEN subtract availability asked with the token fee`() =
            runTest {
                // Arrange — the pair handed to the use case is what decides the reduction; see
                // IsAmountSubtractAvailableUseCaseTest for the decision itself.
                val feeSlot = slot<Pair<CryptoCurrency.ID, Fee>>()
                coEvery {
                    isAmountSubtractAvailableUseCase(any(), any(), capture(feeSlot))
                } returns true.right()

                // Act
                createSendConfirmModel(this, tronGaslessParams())
                advanceUntilIdle()

                // Assert
                assertThat(feeSlot.captured.first).isEqualTo(tronUsdt.id)
                assertThat(feeSlot.captured.second.amount.type).isInstanceOf(AmountType.Token::class.java)
            }

        private fun tronGaslessParams() = MutableParamsContainer(
            defaultSendConfirmParams(
                state = tronGaslessState(),
                cryptoCurrencyStatus = tronUsdtStatus,
                feeCryptoCurrencyStatus = tronUsdtStatus,
            ),
        )
    }

    @Nested
    inner class AddTokenToWallet {

        @BeforeEach
        fun stubSuccessfulAdd() {
            coEvery { sendTransactionUseCase(any(), any(), any()) } returns "txHash".right()
            coEvery {
                manageCryptoCurrenciesUseCase(any(), any<CryptoCurrency>(), any(), any())
            } returns Unit.right()
        }

        @Test
        fun `GIVEN own accounts on several EVM networks WHEN send succeeds THEN token added on its own network`() =
            runTest {
                // Arrange — every EVM account shares one address, and the foreign network is listed first,
                // so matching by address alone would resolve Polygon for a token that lives on Ethereum.
                val networkSlot = slot<Network>()
                every { currenciesRepository.createTokenCurrency(any(), capture(networkSlot)) } returns ethereumUsdc
                val resultFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
                every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns resultFlow
                createSendConfirmModel(this, tokenParams(persistentListOf(polygonRecipient, ethereumRecipient)))
                advanceUntilIdle()

                // Act
                resultFlow.tryEmit(true)
                advanceUntilIdle()

                // Assert
                assertThat(networkSlot.captured).isEqualTo(ethereumUsdc.network)
            }

        @Test
        fun `GIVEN own account only on a foreign network WHEN send succeeds THEN token is not added`() = runTest {
            // Arrange
            val resultFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
            every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns resultFlow
            createSendConfirmModel(this, tokenParams(persistentListOf(polygonRecipient)))
            advanceUntilIdle()

            // Act
            resultFlow.tryEmit(true)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { manageCryptoCurrenciesUseCase(any(), any<CryptoCurrency>(), any(), any()) }
        }

        private fun tokenParams(destinationWallets: ImmutableList<DestinationRecipientListUM>) =
            MutableParamsContainer(
                defaultSendConfirmParams(
                    state = contentState(
                        fee = realFee(),
                        transactionFeeExtended = null,
                        destinationWallets = destinationWallets,
                    ),
                    cryptoCurrencyStatus = com.tangem.features.send.loadedStatus(ethereumUsdc),
                    feeCryptoCurrencyStatus = loadedFeeStatus,
                ),
            )

        private fun recipient(network: Network) = DestinationRecipientListUM(
            id = network.rawId,
            address = DESTINATION_ADDRESS,
            network = network,
            accountId = AccountId.forMainCryptoPortfolio(testUserWalletId),
        )

        private val ethereumRecipient get() = recipient(ethereumUsdc.network)

        private val polygonRecipient get() = recipient(polygonUsdc.network)
    }

    @Nested
    inner class UpdateEditedState {

        @Test
        fun `GIVEN stale parent state WHEN updateEditedState THEN amount and destination applied`() = runTest {
            // Arrange
            val sut = createSendConfirmModel(this, confirmParams(normalFeeState()))
            advanceUntilIdle()
            val editedAmount = mockk<AmountState.Data>(relaxed = true)
            val editedDestination = mockk<DestinationUM.Content>(relaxed = true)

            // Act
            sut.updateEditedState(staleParentState(editedAmount, editedDestination))
            advanceUntilIdle()

            // Assert
            assertThat(sut.uiState.value.amountUM).isEqualTo(editedAmount)
            assertThat(sut.uiState.value.destinationUM).isEqualTo(editedDestination)
            coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerUpdate() }
        }

        @Test
        fun `GIVEN stale parent state WHEN updateEditedState THEN confirm-local state preserved`() = runTest {
            // Arrange: the parent's confirmUM/feeSelectorUM stay Empty/Loading until a successful send —
            // they must not leak into the confirm model (blocks turn unclickable on ConfirmUM.Empty)
            val sut = createSendConfirmModel(this, confirmParams(normalFeeState()))
            advanceUntilIdle()
            val feeSelectorUMBefore = sut.uiState.value.feeSelectorUM

            // Act
            sut.updateEditedState(
                staleParentState(
                    amountUM = mockk<AmountState.Data>(relaxed = true),
                    destinationUM = mockk<DestinationUM.Content>(relaxed = true),
                ),
            )
            advanceUntilIdle()

            // Assert: confirmUM may be recomputed (notifications), but must stay Content — never the
            // parent's Empty, which would disable the confirm blocks; the fee state must survive as is
            assertThat(sut.uiState.value.confirmUM).isInstanceOf(ConfirmUM.Content::class.java)
            assertThat(sut.uiState.value.feeSelectorUM).isEqualTo(feeSelectorUMBefore)
        }

        private fun staleParentState(amountUM: AmountState, destinationUM: DestinationUM) = SendUM(
            amountUM = amountUM,
            destinationUM = destinationUM,
            feeSelectorUM = FeeSelectorUM.Loading,
            confirmUM = ConfirmUM.Empty,
            confirmData = null,
        )
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

    /**
     * Max send of a Tron token whose gasless compensation is charged to that same token: the entered
     * amount is the whole balance and the fee is a token-denominated [Fee.Common] carrying a quote.
     */
    private fun tronGaslessState(): SendUM = contentState(
        fee = tronGaslessFee(),
        transactionFeeExtended = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = tronGaslessFee()),
            feeTokenId = tronUsdt.id,
            tronGaslessQuote = mockk(relaxed = true),
        ),
        enteredAmount = TRON_BALANCE,
        feeCryptoCurrencyStatus = tronUsdtStatus,
    )

    private fun contentState(
        fee: Fee,
        transactionFeeExtended: TransactionFeeExtended?,
        enteredAmount: BigDecimal = BigDecimal.ONE,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus = loadedFeeStatus,
        destinationWallets: ImmutableList<DestinationRecipientListUM> = persistentListOf(),
    ): SendUM {
        val amount = mockk<AmountState.Data>(relaxed = true) {
            every { amountTextField.cryptoAmount.value } returns enteredAmount
            every { reduceAmountBy } returns BigDecimal.ZERO
            every { isIgnoreReduce } returns false
        }
        val destination = mockk<DestinationUM.Content>(relaxed = true) {
            every { addressTextField.actualAddress } returns DESTINATION_ADDRESS
            every { memoTextField } returns null
            every { wallets } returns destinationWallets
        }
        val extraInfo = mockk<FeeExtraInfo>(relaxed = true) {
            every { this@mockk.transactionFeeExtended } returns transactionFeeExtended
            every { this@mockk.feeCryptoCurrencyStatus } returns feeCryptoCurrencyStatus
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

    /** Real USDC contracts — the two that got mixed across networks in [REDACTED_TASK_KEY]. */
    private val ethereumUsdc: CryptoCurrency.Token = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Ethereum,
        id = "usd-coin",
        contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
    )

    private val polygonUsdc: CryptoCurrency.Token = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Polygon,
        id = "usd-coin",
        contractAddress = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359",
    )

    private val tronUsdt: CryptoCurrency.Token = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Tron,
        id = "tether",
        contractAddress = "TUsdt",
    )

    private val tronUsdtStatus: CryptoCurrencyStatus
        get() = com.tangem.features.send.loadedStatus(tronUsdt, balance = TRON_BALANCE)

    /** Tron gasless denominates the compensation in a token but ships it as a plain [Fee.Common]. */
    private fun tronGaslessFee(): Fee = Fee.Common(
        Amount(
            token = Token(
                symbol = tronUsdt.symbol,
                contractAddress = tronUsdt.contractAddress,
                decimals = tronUsdt.decimals,
            ),
            value = TRON_GASLESS_FEE,
        ),
    )

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

    private companion object {
        val TRON_BALANCE: BigDecimal = BigDecimal("24.929183")
        val TRON_GASLESS_FEE: BigDecimal = BigDecimal("2.51")
        const val DESTINATION_ADDRESS = "destinationAddr"
    }

    // endregion
}