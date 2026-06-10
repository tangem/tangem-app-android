package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
            amount = any(), fee = any(), memo = any(),
            destination = any(), userWalletId = any(), network = any(), txExtras = any(),
        )
    }

    // endregion
}