package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.FeeBucket
import com.tangem.feature.swap.domain.models.ui.IntegratedApprovalData
import com.tangem.feature.swap.domain.models.ui.SwapFee
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Covers the `onSwap` flow resolution added for swap-xyz: for DEX / DEX_BRIDGE providers the
 * executed path is chosen by the shape of `swapData.transaction`, not by `provider.type`:
 *  - `ExpressTransactionModel.DEX`        -> DEX path  (`createTransactionUseCase`, no `getExchangeData`)
 *  - `ExpressTransactionModel.CEX` / null -> CEX path  (`repository.getExchangeData`)
 *
 * Routing is asserted by side-effects only: the CEX path always re-fetches via `getExchangeData`,
 * the DEX path never does. The CEX/DEX terminal calls are stubbed to fail fast (Left) so the test
 * stays focused on the dispatch decision and needs no full send wiring.
 *
 * Existing real-CEX behavior is kept as a regression guard.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplOnSwapTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()

    @BeforeEach
    fun setup() {
        every { isDemoCardUseCase(any()) } returns false
        // CEX path: return early on a Left so we only observe the getExchangeData side-effect.
        coEvery {
            repository.getExchangeData(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                providerId = any(), rateType = any(), toAddress = any(),
                expressOperationType = any(), refundAddress = any(),
            )
        } returns ExpressDataError.UnknownError().left()
        // DEX path: extras must resolve (createDexTxExtras errors on null), then createTransaction
        // returns a Left so onSwapDex returns early after the call is recorded.
        coEvery {
            createTransactionExtrasUseCase(data = any(), network = any(), gasLimit = any())
        } returns mockk<TransactionExtras>(relaxed = true).right()
        coEvery {
            createTransactionUseCase(
                amount = any(), fee = any(), memo = any(),
                destination = any(), userWalletId = any(), network = any(), txExtras = any(),
            )
        } returns Throwable("stub").left()
    }

    @Test
    fun `GIVEN DEX provider with DEX swapData WHEN onSwap THEN takes DEX path`() = runTest {
        onSwap(provider = ExchangeProviderType.DEX, swapData = dexSwapData())

        coVerifyCreateTransaction(times = 1)
        coVerifyGetExchangeData(times = 0)
    }

    @Test
    fun `GIVEN DEX provider with CEX swapData WHEN onSwap THEN takes CEX path`() = runTest {
        onSwap(provider = ExchangeProviderType.DEX, swapData = cexSwapData())

        coVerifyGetExchangeData(times = 1)
        coVerifyCreateTransaction(times = 0)
    }

    @Test
    fun `GIVEN DEX provider with null swapData WHEN onSwap THEN takes CEX path`() = runTest {
        // The bridge re-route nulled swapData at the quote stage; onSwap must fall through to CEX.
        onSwap(provider = ExchangeProviderType.DEX, swapData = null)

        coVerifyGetExchangeData(times = 1)
        coVerifyCreateTransaction(times = 0)
    }

    @Test
    fun `GIVEN DEX_BRIDGE provider with DEX swapData WHEN onSwap THEN takes DEX path`() = runTest {
        onSwap(provider = ExchangeProviderType.DEX_BRIDGE, swapData = dexSwapData())

        coVerifyCreateTransaction(times = 1)
        coVerifyGetExchangeData(times = 0)
    }

    @Test
    fun `GIVEN CEX provider WHEN onSwap THEN takes CEX path`() = runTest {
        // Regression guard: real CEX provider is unaffected by the new resolution.
        onSwap(provider = ExchangeProviderType.CEX, swapData = null)

        coVerifyGetExchangeData(times = 1)
    }

    // region integrated approve+swap (sendIntegratedApproveAndSwap)

    @Test
    fun `GIVEN integratedApproval WHEN onSwap THEN sends approve plus swap as one DEFAULT batch and swap hash is last`() =
        runTest {
            stubSwapTxCreated()
            val txsSlot = slot<List<TransactionData>>()
            coEvery {
                sendTransactionUseCase(
                    txsData = capture(txsSlot),
                    userWallet = any(),
                    network = any(),
                    sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
                )
            } returns listOf(APPROVAL_HASH, SWAP_HASH).right()

            val result = onSwapIntegrated(integratedApproval = integratedApproval(approvalFee = singleFee()))

            // approval tx first, swap tx last → 2 txs in a single batch.
            assertThat(txsSlot.captured).hasSize(2)
            assertThat(txsSlot.captured.first()).isInstanceOf(TransactionData.Uncompiled::class.java)
            // Success carries the LAST hash (the swap tx); approval hash is dropped.
            assertThat(result).isInstanceOf(SwapTransactionState.TxSent::class.java)
            assertThat((result as SwapTransactionState.TxSent).txHash).isEqualTo(SWAP_HASH)
            coVerify(exactly = 1) {
                sendTransactionUseCase(
                    txsData = any(),
                    userWallet = any(),
                    network = any(),
                    sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
                )
            }
        }

    @Test
    fun `GIVEN integratedApproval AND send fails WHEN onSwap THEN returns TransactionError`() = runTest {
        stubSwapTxCreated()
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns SendTransactionError.UnknownError(Exception("boom")).left()

        val result = onSwapIntegrated(integratedApproval = integratedApproval(approvalFee = singleFee()))

        assertThat(result).isInstanceOf(SwapTransactionState.Error.TransactionError::class.java)
    }

    @Test
    fun `GIVEN Choosable approval fee AND SLOW bucket THEN approval tx fee is the minimum`() = runTest {
        assertApprovalFeeBucket(
            approvalFee = choosableFee(),
            bucket = FeeBucket.SLOW,
            expectedFee = MIN_FEE,
        )
    }

    @Test
    fun `GIVEN Choosable approval fee AND FAST bucket THEN approval tx fee is the priority`() = runTest {
        assertApprovalFeeBucket(
            approvalFee = choosableFee(),
            bucket = FeeBucket.FAST,
            expectedFee = PRIORITY_FEE,
        )
    }

    @Test
    fun `GIVEN Choosable approval fee AND MARKET bucket THEN approval tx fee is the normal`() = runTest {
        assertApprovalFeeBucket(
            approvalFee = choosableFee(),
            bucket = FeeBucket.MARKET,
            expectedFee = NORMAL_FEE,
        )
    }

    @Test
    fun `GIVEN Single approval fee THEN approval tx fee is the normal regardless of bucket`() = runTest {
        assertApprovalFeeBucket(
            approvalFee = singleFee(),
            bucket = FeeBucket.SLOW,
            expectedFee = NORMAL_FEE,
        )
    }

    private suspend fun assertApprovalFeeBucket(
        approvalFee: TransactionFee,
        bucket: FeeBucket,
        expectedFee: Fee,
    ) {
        stubSwapTxCreated()
        val txsSlot = slot<List<TransactionData>>()
        coEvery {
            sendTransactionUseCase(txsData = capture(txsSlot), userWallet = any(), network = any(), sendMode = any())
        } returns listOf(APPROVAL_HASH, SWAP_HASH).right()

        onSwapIntegrated(
            integratedApproval = integratedApproval(approvalFee = approvalFee),
            swapFee = buildSwapFee(feeBucket = bucket),
        )

        val approvalTx = txsSlot.captured.first() as TransactionData.Uncompiled
        assertThat(approvalTx.fee).isEqualTo(expectedFee)
    }

    private fun stubSwapTxCreated() {
        // The swap tx must be a real Uncompiled so getPayoutAddress(swapTxData) resolves.
        coEvery {
            createTransactionUseCase(
                amount = any(), fee = any(), memo = any(),
                destination = any(), userWalletId = any(), network = any(), txExtras = any(),
            )
        } returns swapTxUncompiled().right()
    }

    private suspend fun onSwapIntegrated(
        integratedApproval: IntegratedApprovalData,
        swapFee: SwapFee = buildSwapFee(feeBucket = FeeBucket.MARKET),
    ): SwapTransactionState = sut.onSwap(
        fromSwapCurrencyStatus = hotStatus(),
        toSwapCurrencyStatus = hotStatus(),
        swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
        swapData = dexSwapData(),
        amountToSwap = "1.0",
        balanceStatus = SwapBalanceStatus.Sufficient,
        fee = swapFee,
        expressOperationType = ExpressOperationType.SWAP,
        isTangemPayWithdrawal = false,
        integratedApproval = integratedApproval,
    )

    private fun integratedApproval(approvalFee: TransactionFee): IntegratedApprovalData = IntegratedApprovalData(
        approvalTransaction = approvalTxUncompiled(),
        approvalFee = approvalFee,
        approveType = ApproveType.UNLIMITED,
    )

    private fun approvalTxUncompiled(): TransactionData.Uncompiled = TransactionData.Uncompiled(
        amount = realAmount(),
        fee = null,
        sourceAddress = "0xFrom",
        destinationAddress = "0xContract",
    )

    private fun swapTxUncompiled(): TransactionData.Uncompiled = TransactionData.Uncompiled(
        amount = realAmount(),
        fee = NORMAL_FEE,
        sourceAddress = "0xFrom",
        destinationAddress = "0xTo",
    )

    private fun realAmount(): Amount = Amount(
        currencySymbol = "ETH",
        value = BigDecimal.ONE,
        decimals = 18,
    )

    private fun singleFee(): TransactionFee.Single = TransactionFee.Single(normal = NORMAL_FEE)

    private fun choosableFee(): TransactionFee.Choosable = TransactionFee.Choosable(
        minimum = MIN_FEE,
        normal = NORMAL_FEE,
        priority = PRIORITY_FEE,
    )

    // endregion

    // region helpers

    private suspend fun onSwap(provider: ExchangeProviderType, swapData: SwapDataModel?) {
        sut.onSwap(
            fromSwapCurrencyStatus = hotStatus(),
            toSwapCurrencyStatus = hotStatus(),
            swapProvider = buildSwapProvider(provider),
            swapData = swapData,
            amountToSwap = "1.0",
            balanceStatus = SwapBalanceStatus.Sufficient,
            fee = buildSwapFee(),
            expressOperationType = ExpressOperationType.SWAP,
            isTangemPayWithdrawal = false,
        )
    }

    /** Backed by an explicit [UserWallet.Hot] mock so the `is UserWallet.Cold` demo check is false. */
    private fun hotStatus(): SwapCurrencyStatus {
        val hotWallet = mockk<UserWallet.Hot>(relaxed = true)
        return buildSwapCurrencyStatus(networkRawId = ethNetwork).let {
            SwapCurrencyStatus(userWallet = hotWallet, status = it.status, account = it.account)
        }
    }

    private fun dexSwapData(): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
        transaction = ExpressTransactionModel.DEX(
            fromAmount = SwapAmount(BigDecimal("1.0"), 18),
            toAmount = SwapAmount(BigDecimal("0.5"), 18),
            txValue = "1000000000000000000",
            txId = "tx-id",
            txTo = "0xTo",
            txExtraId = null,
            txFrom = "0xFrom",
            txData = "0xdata",
            otherNativeFeeWei = null,
            gas = BigInteger.valueOf(21_000L),
            allowanceContract = null,
        ),
    )

    private fun cexSwapData(): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
        transaction = ExpressTransactionModel.CEX(
            fromAmount = SwapAmount(BigDecimal("1.0"), 18),
            toAmount = SwapAmount(BigDecimal("0.5"), 18),
            txValue = null,
            txId = "cex-tx-id",
            txTo = "0xCexAddress",
            txExtraId = null,
            externalTxId = "ext-id",
            externalTxUrl = "https://explorer/tx",
            txExtraIdName = null,
        ),
    )

    private fun coVerifyGetExchangeData(times: Int) = coVerify(exactly = times) {
        repository.getExchangeData(
            userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
            toContractAddress = any(), fromAddress = any(), toNetwork = any(),
            fromAmount = any(), fromDecimals = any(), toDecimals = any(),
            providerId = any(), rateType = any(), toAddress = any(),
            expressOperationType = any(), refundAddress = any(),
        )
    }

    private fun coVerifyCreateTransaction(times: Int) = coVerify(exactly = times) {
        createTransactionUseCase(
            amount = any(),
            fee = any(),
            memo = any(),
            destination = any(),
            userWalletId = any(),
            network = any(),
            txExtras = any(),
        )
    }

    // endregion

    private companion object {
        const val APPROVAL_HASH = "0xApprovalHash"
        const val SWAP_HASH = "0xSwapHash"

        val MIN_FEE: Fee = feeOf(BigDecimal("0.001"))
        val NORMAL_FEE: Fee = feeOf(BigDecimal("0.002"))
        val PRIORITY_FEE: Fee = feeOf(BigDecimal("0.003"))

        private fun feeOf(value: BigDecimal): Fee = Fee.Common(
            amount = Amount(currencySymbol = "ETH", value = value, decimals = 18),
        )
    }
}