package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.feature.swap.domain.fee.CexFeeResult
import com.tangem.feature.swap.domain.fee.DexFeeResult
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.test.core.ProvideTestModels
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Covers the branch a DEX provider takes when it returns a plain ERC-20 `transfer` instead of swap
 * call data for a token with an active Yield Mode: the tokens are held by the yield module, so the
 * module has to perform the transfer itself via `send()`.
 *
 * The call data is decoded by `TransferERC20TokenCallData` at the two stages whose work depends on it,
 * and each is asserted through the branch it selects:
 *  - fee   ([SwapInteractorImpl.loadSwapFee])  — `CexSwapFeeCalculator` instead of `DexSwapFeeCalculator`,
 *    plus the gate that refuses a payload whose destination and call data disagree
 *  - send  (`onSwapDex`)                       — `CreateTransferTransactionUseCase` instead of the
 *    `buildYieldSwapCallData` wrapper, and `payInAddress` decoded from the module's send call data
 *
 * A real yield swap and a non-yield swap are kept as regression guards: neither may be re-routed.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplYieldSendTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()

    @BeforeEach
    fun setup() {
        every { isDemoCardUseCase(any()) } returns false
        every { swapFeatureToggles.isYieldSwapEnabled } returns true
        every { swapFeatureToggles.isYieldDexTransferEnabled } returns true
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns mockk<
            com.tangem.domain.models.currency.CryptoCurrencyStatus,
            >(relaxed = true).right()
        coEvery { yieldModuleAddressProvider.getOrFetch(any(), any()) } returns YIELD_MODULE
        coEvery {
            cexSwapFeeCalculator.calculate(any(), any(), any(), any(), any())
        } returns CexFeeResult(transactionFee = loadedFee()).right()
        coEvery {
            dexSwapFeeCalculator.calculate(any(), any(), any(), any())
        } returns dexFeeResult().right()
        coEvery {
            dexSwapFeeCalculator.calculateYield(any(), any(), any())
        } returns dexFeeResult().right()
    }

    // region fee stage

    @Test
    fun `GIVEN yield token and transfer calldata WHEN loadSwapFee THEN priced by the CEX calculator`() = runTest {
        // Act
        val result = loadSwapFee(fromStatus = yieldTokenStatus(), swapData = transferSwapData())

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { cexSwapFeeCalculator.calculate(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { dexSwapFeeCalculator.calculateYield(any(), any(), any()) }
        coVerify(exactly = 0) { dexSwapFeeCalculator.calculate(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN yield token and swap calldata WHEN loadSwapFee THEN priced by the yield DEX calculator`() = runTest {
        // Act — regression guard: a real yield swap must keep the module `swap()` pricing
        val result = loadSwapFee(fromStatus = yieldTokenStatus(), swapData = swapCallDataSwapData())

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { dexSwapFeeCalculator.calculateYield(any(), any(), any()) }
        coVerify(exactly = 0) { cexSwapFeeCalculator.calculate(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN non-yield token and transfer calldata WHEN loadSwapFee THEN priced by the plain DEX calculator`() =
        runTest {
            // Act
            val fromStatus = tokenStatus(yieldSupplyActive = false)
            val result = loadSwapFee(fromStatus = fromStatus, swapData = transferSwapData())

            // Assert
            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) { dexSwapFeeCalculator.calculate(any(), any(), any(), any()) }
            coVerify(exactly = 0) { cexSwapFeeCalculator.calculate(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN toggle off and transfer calldata WHEN loadSwapFee THEN priced by the yield DEX calculator`() = runTest {
        // Arrange
        every { swapFeatureToggles.isYieldDexTransferEnabled } returns false

        // Act
        val result = loadSwapFee(fromStatus = yieldTokenStatus(), swapData = transferSwapData())

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { dexSwapFeeCalculator.calculateYield(any(), any(), any()) }
        coVerify(exactly = 0) { cexSwapFeeCalculator.calculate(any(), any(), any(), any(), any()) }
    }

    // endregion

    // region send stage

    @Test
    fun `GIVEN yield token and transfer calldata WHEN onSwap THEN transfer is built for the decoded recipient`() =
        runTest {
            // Arrange
            val amountSlot = slot<Amount>()
            val destinationSlot = slot<String>()
            coEvery {
                createTransferTransactionUseCase(
                    amount = capture(amountSlot),
                    fee = any(),
                    memo = any(),
                    destination = capture(destinationSlot),
                    userWalletId = any(),
                    network = any(),
                )
            } returns Throwable("stub").left()

            // Act
            onSwap(fromStatus = yieldTokenStatus(), swapData = transferSwapData())

            // Assert — recipient and amount come from the call data, not from `txTo` / the quote
            assertThat(destinationSlot.captured).isEqualTo(RECIPIENT)
            assertThat(amountSlot.captured.value).isEqualTo(BigDecimal.ONE.setScale(DECIMALS))
            coVerify(exactly = 0) { dexSwapFeeCalculator.buildYieldSwapCallData(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN yield token and swap calldata WHEN onSwap THEN the module swap wrapper is built`() = runTest {
        // Arrange — regression guard: a real yield swap still goes through `buildYieldSwapCallData`
        coEvery {
            dexSwapFeeCalculator.buildYieldSwapCallData(any(), any(), any(), any(), any())
        } returns mockk(relaxed = true)
        // The wrapper is turned into tx extras before the transaction is built; a relaxed answer is
        // not castable to TransactionExtras, so it has to be stubbed explicitly.
        every {
            createTransactionExtrasUseCase(callData = any(), network = any(), gasLimit = any(), nonce = any())
        } returns mockk<TransactionExtras>(relaxed = true).right()
        // Terminal call of the yield-swap builder — fail fast so the test observes the dispatch only.
        coEvery {
            createTransactionUseCase(
                amount = any(), fee = any(), memo = any(), destination = any(),
                userWalletId = any(), network = any(), txExtras = any(),
            )
        } returns Throwable("stub").left()

        // Act
        onSwap(fromStatus = yieldTokenStatus(), swapData = swapCallDataSwapData())

        // Assert
        coVerify(exactly = 1) { dexSwapFeeCalculator.buildYieldSwapCallData(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) {
            createTransferTransactionUseCase(
                amount = any(), fee = any(), memo = any(),
                destination = any(), userWalletId = any(), network = any(),
            )
        }
    }

    @Test
    fun `GIVEN toggle off and transfer calldata WHEN onSwap THEN the module swap wrapper is built`() = runTest {
        // Arrange
        every { swapFeatureToggles.isYieldDexTransferEnabled } returns false
        coEvery {
            dexSwapFeeCalculator.buildYieldSwapCallData(any(), any(), any(), any(), any())
        } returns mockk(relaxed = true)
        every {
            createTransactionExtrasUseCase(callData = any(), network = any(), gasLimit = any(), nonce = any())
        } returns mockk<TransactionExtras>(relaxed = true).right()
        coEvery {
            createTransactionUseCase(
                amount = any(), fee = any(), memo = any(), destination = any(),
                userWalletId = any(), network = any(), txExtras = any(),
            )
        } returns Throwable("stub").left()

        // Act
        onSwap(fromStatus = yieldTokenStatus(), swapData = transferSwapData())

        // Assert
        coVerify(exactly = 1) { dexSwapFeeCalculator.buildYieldSwapCallData(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) {
            createTransferTransactionUseCase(
                amount = any(), fee = any(), memo = any(),
                destination = any(), userWalletId = any(), network = any(),
            )
        }
    }

    @Test
    fun `GIVEN transfer sent by the module WHEN onSwap THEN payInAddress is the recipient not the token`() = runTest {
        // Arrange — the SDK re-targets the transaction to the module and keeps the recipient in the call data
        coEvery {
            createTransferTransactionUseCase(
                amount = any(), fee = any(), memo = any(),
                destination = any(), userWalletId = any(), network = any(),
            )
        } returns moduleSendTxData().right()
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } returns TX_HASH.right()

        // Act
        val result = onSwap(fromStatus = yieldTokenStatus(), swapData = transferSwapData())

        // Assert
        assertThat(result).isInstanceOf(SwapTransactionState.TxSent::class.java)
        coVerify(exactly = 1) {
            repository.exchangeSent(
                userWallet = any(),
                txId = any(),
                fromNetwork = any(),
                fromAddress = any(),
                payInAddress = RECIPIENT,
                txHash = TX_HASH,
                payInExtraId = any(),
            )
        }
    }

    // endregion

    // region the gate

    @ParameterizedTest
    @ProvideTestModels
    fun isYieldPayloadConsistent(model: ConsistencyModel) {
        // Arrange
        every { swapFeatureToggles.isYieldDexTransferEnabled } returns model.isToggleEnabled

        // Act
        val actual = sut.isYieldPayloadConsistent(
            fromSwapCurrencyStatus = tokenStatus(yieldSupplyActive = model.isYieldActive),
            swapData = model.txData?.let { swapDataOf(txTo = model.txTo, txData = it) },
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        // not the token, selector is not `transfer` → the provider's swap call data, nothing to refine
        ConsistencyModel(txTo = DEX_ROUTER, txData = "0x$SWAP_SELECTOR$RECIPIENT_WORD$AMOUNT_WORD", expected = true),
        // the token, call data decodes as `transfer` → the module sends its own tokens
        ConsistencyModel(
            txTo = TOKEN_CONTRACT,
            txData = "0x$TRANSFER_SELECTOR$RECIPIENT_WORD$AMOUNT_WORD",
            expected = true,
        ),
        // the token, selector is not `transfer` → an unknown method invoked on the token contract
        ConsistencyModel(txTo = TOKEN_CONTRACT, txData = "0x$SWAP_SELECTOR$RECIPIENT_WORD$AMOUNT_WORD"),
        // not the token, selector is `transfer` → a transfer aimed at something else
        ConsistencyModel(txTo = DEX_ROUTER, txData = "0x$TRANSFER_SELECTOR$RECIPIENT_WORD$AMOUNT_WORD"),
        // the token, selector is `transfer`, arguments are not 64 bytes → the decoder refuses them
        ConsistencyModel(
            txTo = TOKEN_CONTRACT,
            txData = "0x$TRANSFER_SELECTOR$RECIPIENT_WORD${AMOUNT_WORD}deadbeef",
        ),
        // the refinement does not apply — every guard in front of the two rules
        ConsistencyModel(
            txTo = TOKEN_CONTRACT,
            txData = "0x$SWAP_SELECTOR$RECIPIENT_WORD$AMOUNT_WORD",
            isToggleEnabled = false,
            expected = true,
        ),
        ConsistencyModel(
            txTo = TOKEN_CONTRACT,
            txData = "0x$SWAP_SELECTOR$RECIPIENT_WORD$AMOUNT_WORD",
            isYieldActive = false,
            expected = true,
        ),
        ConsistencyModel(txTo = TOKEN_CONTRACT, txData = null, expected = true),
    )

    internal data class ConsistencyModel(
        val txTo: String,
        val txData: String?,
        val expected: Boolean = false,
        val isToggleEnabled: Boolean = true,
        val isYieldActive: Boolean = true,
    )

    @Test
    fun `GIVEN toggle off and contradictory calldata WHEN loadSwapFee THEN priced by the yield DEX calculator`() =
        runTest {
            // Arrange
            every { swapFeatureToggles.isYieldDexTransferEnabled } returns false
            val swapData = transferSwapData(txData = "0x$SWAP_SELECTOR$RECIPIENT_WORD$AMOUNT_WORD")

            // Act
            val result = loadSwapFee(fromStatus = yieldTokenStatus(), swapData = swapData)

            // Assert
            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) { dexSwapFeeCalculator.calculateYield(any(), any(), any()) }
        }

    // endregion

    // region helpers

    private suspend fun loadSwapFee(fromStatus: SwapCurrencyStatus, swapData: SwapDataModel) = sut.loadSwapFee(
        quotesLoadedState = buildQuotesLoadedState(fromStatus),
        fromStatus = fromStatus,
        toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork),
        amount = SwapAmount(BigDecimal.ONE, DECIMALS),
        swapData = swapData,
        selectedFeeToken = null,
        isGasless = false,
        txType = ExpressTxType.SWAP,
    )

    private suspend fun onSwap(fromStatus: SwapCurrencyStatus, swapData: SwapDataModel) = sut.onSwap(
        fromSwapCurrencyStatus = fromStatus,
        toSwapCurrencyStatus = fromStatus,
        swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
        swapData = swapData,
        amountToSwap = "1.0",
        balanceStatus = SwapBalanceStatus.Sufficient,
        fee = buildSwapFee(),
        expressOperationType = ExpressOperationType.SWAP,
        isTangemPayWithdrawal = false,
    )

    /** Backed by an explicit [UserWallet.Hot] mock so the `is UserWallet.Cold` demo check is false. */
    private fun tokenStatus(yieldSupplyActive: Boolean): SwapCurrencyStatus {
        val hotWallet = mockk<UserWallet.Hot>(relaxed = true)
        return buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            contractAddress = TOKEN_CONTRACT,
            isCoin = false,
            decimals = DECIMALS,
            yieldSupplyActive = yieldSupplyActive,
        ).let { SwapCurrencyStatus(userWallet = hotWallet, status = it.status, account = it.account) }
    }

    private fun yieldTokenStatus(): SwapCurrencyStatus = tokenStatus(yieldSupplyActive = true)

    /** A provider transaction addressed to the swapped token, carrying an ERC-20 `transfer`. */
    private fun transferSwapData(
        txData: String = "0x$TRANSFER_SELECTOR$RECIPIENT_WORD$AMOUNT_WORD",
    ): SwapDataModel = swapDataOf(txTo = TOKEN_CONTRACT, txData = txData)

    /** A provider transaction addressed to a router, carrying real swap call data. */
    private fun swapCallDataSwapData(): SwapDataModel =
        swapDataOf(txTo = DEX_ROUTER, txData = "0x$SWAP_SELECTOR$RECIPIENT_WORD$AMOUNT_WORD")

    private fun swapDataOf(txTo: String, txData: String): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(BigDecimal("0.5"), DECIMALS),
        transaction = ExpressTransactionModel.DEX(
            fromAmount = SwapAmount(BigDecimal.ONE, DECIMALS),
            toAmount = SwapAmount(BigDecimal("0.5"), DECIMALS),
            txValue = "0",
            txId = "tx-id",
            txTo = txTo,
            txExtraId = null,
            txFrom = "0xFrom",
            txData = txData,
            otherNativeFeeWei = null,
            gas = BigInteger.valueOf(21_000L),
            allowanceContract = DEX_ROUTER,
        ),
    )

    /**
     * What the SDK produces for `AmountType.TokenYieldSupply`: the transaction is addressed to the
     * yield module and the real recipient lives inside [EthereumYieldSupplySendCallData].
     */
    private fun moduleSendTxData(): TransactionData.Uncompiled = TransactionData.Uncompiled(
        amount = Amount(currencySymbol = "USDC", value = BigDecimal.ONE, decimals = DECIMALS),
        fee = null,
        sourceAddress = "0xFrom",
        destinationAddress = YIELD_MODULE,
        extras = EthereumTransactionExtras(
            callData = EthereumYieldSupplySendCallData(
                tokenContractAddress = TOKEN_CONTRACT,
                destinationAddress = RECIPIENT,
                amount = Amount(currencySymbol = "USDC", value = BigDecimal.ONE, decimals = DECIMALS),
            ),
        ),
    )

    private fun buildQuotesLoadedState(fromStatus: SwapCurrencyStatus): SwapState.QuotesLoadedState =
        SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal.ONE, DECIMALS),
                swapCurrencyStatus = fromStatus,
                amountFiat = BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal("0.5"), DECIMALS),
                swapCurrencyStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork),
                amountFiat = BigDecimal.ZERO,
            ),
            priceImpact = PriceImpact.Empty,
            swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
            minAdaValue = null,
            txType = ExpressTxType.SWAP,
        )

    private fun loadedFee() = TransactionFeeResult.Loaded(mockk<TransactionFee.Single>(relaxed = true))

    private fun dexFeeResult() = DexFeeResult(
        transactionFee = loadedFee(),
        otherNativeFee = BigDecimal.ZERO,
        gas = BigInteger.valueOf(21_000L),
    )

    // endregion

    private companion object {
        const val TOKEN_CONTRACT = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val DEX_ROUTER = "0x1111111254eeb25477b68fb85ed929f73a960582"
        const val RECIPIENT = "0x742d35cc6634c0532925a3b844bc454e4438f44e"
        const val YIELD_MODULE = "0x00000000000000000000000000000000000module"
        const val TX_HASH = "0xTxHash"

        const val SWAP_SELECTOR = "12aa3caf"
        const val DECIMALS = 6
        const val WORD_HEX_LENGTH = 64

        val TRANSFER_SELECTOR: String = TransferERC20TokenCallData.METHOD_ID.removePrefix("0x")

        /** `RECIPIENT` left-padded to a 32-byte ABI word. */
        val RECIPIENT_WORD: String = RECIPIENT.removePrefix("0x").padStart(WORD_HEX_LENGTH, '0')

        /** 1 token with [DECIMALS] decimals, as a 32-byte ABI word. */
        val AMOUNT_WORD: String = BigInteger.valueOf(1_000_000L).toString(16).padStart(WORD_HEX_LENGTH, '0')
    }
}