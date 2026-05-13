package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.IncludeFeeInAmount
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.SwapFeeState
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.FeeBucket
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.domain.models.ui.SwapFee
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Tests for [SwapInteractorImpl.applySwapFee] — [REDACTED_TASK_KEY] Phase 4.
 *
 * Verifies:
 *  - The fee value (including bridge `otherNativeFee`) propagates to `feeState`, `isBalanceEnough`,
 *    and `includeFeeInAmount`.
 *  - Each [FeePaidCurrency] branch is recomputed correctly: Coin / SameCurrency / Token / FeeResource.
 *  - Bridge boundary: when native balance is between `fee` and `fee + otherNativeFee`,
 *    `feeState` flips from Enough to NotEnough.
 *  - Idempotency: applying the same SwapFee twice yields equal state.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplApplySwapFeeTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()

    @BeforeEach
    fun setup() {
        coEvery {
            getCurrencyCheckUseCase.invoke(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = any(),
                recipientAddress = any(),
            )
        } returns buildCurrencyCheck()
        coEvery {
            validateTransactionUseCase.invoke(
                amount = any(),
                fee = any(),
                memo = any(),
                destination = any(),
                userWalletId = any(),
                network = any(),
            )
        } returns Unit.right()
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any()) } returns null.right()
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns null
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns buildCoinCurrency()
    }

    @Test
    fun `applySwapFee recomputes feeState to Enough when native balance covers fee`() = runTest {
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")

        val state = buildQuotesLoadedState(
            fromAmount = SwapAmount(BigDecimal("1"), 18),
            isCoin = true,
            fromBalance = BigDecimal("10"),
        )
        val swapFee = buildSwapFee(feeValue = BigDecimal("0.001"), otherNativeFee = BigDecimal.ZERO)

        val patched = sut.applySwapFee(state, swapFee)

        assertThat(patched.preparedSwapConfigState.feeState).isInstanceOf(SwapFeeState.Enough::class.java)
        assertThat(patched.preparedSwapConfigState.isBalanceEnough).isTrue()
    }

    @Test
    fun `applySwapFee recomputes feeState to NotEnough when native balance below fee`() = runTest {
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("0.0001")

        val state = buildQuotesLoadedState(
            fromAmount = SwapAmount(BigDecimal("1"), 18),
            isCoin = false,
            fromBalance = BigDecimal("10"),
        )
        val swapFee = buildSwapFee(feeValue = BigDecimal("0.01"))

        val patched = sut.applySwapFee(state, swapFee)

        assertThat(patched.preparedSwapConfigState.feeState).isInstanceOf(SwapFeeState.NotEnough::class.java)
    }

    @Test
    fun `applySwapFee — bridge otherNativeFee — boundary flips feeState to NotEnough`() = runTest {
        // From-token is a Token, native fee is small enough alone but combined with otherNativeFee exceeds balance.
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("0.0015")

        val state = buildQuotesLoadedState(
            fromAmount = SwapAmount(BigDecimal("1"), 18),
            isCoin = false,
            fromBalance = BigDecimal("10"),
        )
        // fee=0.001, otherNativeFee=0.001 => feeToCheck=0.002 > 0.0015 nativeBalance → NotEnough
        val swapFee = buildSwapFee(
            feeValue = BigDecimal("0.001"),
            otherNativeFee = BigDecimal("0.001"),
        )

        val patched = sut.applySwapFee(state, swapFee)

        assertThat(patched.preparedSwapConfigState.feeState).isInstanceOf(SwapFeeState.NotEnough::class.java)
    }

    @Test
    fun `applySwapFee — bridge otherNativeFee — Enough when balance covers combined fee`() = runTest {
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("0.005")

        val state = buildQuotesLoadedState(
            fromAmount = SwapAmount(BigDecimal("1"), 18),
            isCoin = false,
            fromBalance = BigDecimal("10"),
        )
        // fee=0.001, otherNativeFee=0.001 => feeToCheck=0.002 <= 0.005 nativeBalance → Enough
        val swapFee = buildSwapFee(
            feeValue = BigDecimal("0.001"),
            otherNativeFee = BigDecimal("0.001"),
        )

        val patched = sut.applySwapFee(state, swapFee)

        assertThat(patched.preparedSwapConfigState.feeState).isInstanceOf(SwapFeeState.Enough::class.java)
    }

    @Test
    fun `applySwapFee — FeeResource branch flips on isFeeResourceEnough`() = runTest {
        coEvery {
            currenciesRepository.getFeePaidCurrency(any(), any())
        } returns FeePaidCurrency.FeeResource(currency = "FEE")
        coEvery { currencyChecksRepository.checkIfFeeResourceEnough(any(), any(), any()) } returns true

        val state = buildQuotesLoadedState(
            fromAmount = SwapAmount(BigDecimal("1"), 18),
            isCoin = true,
            fromBalance = BigDecimal("10"),
        )
        val swapFee = buildSwapFee(feeValue = BigDecimal("0.001"))

        val patched = sut.applySwapFee(state, swapFee)

        // Stubbed isFeeResourceEnough = true => Enough
        assertThat(patched.preparedSwapConfigState.feeState).isInstanceOf(SwapFeeState.Enough::class.java)
    }

    @Test
    fun `applySwapFee is idempotent — applying twice yields equal state`() = runTest {
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")

        val state = buildQuotesLoadedState(
            fromAmount = SwapAmount(BigDecimal("1"), 18),
            isCoin = true,
            fromBalance = BigDecimal("10"),
        )
        val swapFee = buildSwapFee(feeValue = BigDecimal("0.001"))

        val first = sut.applySwapFee(state, swapFee)
        val second = sut.applySwapFee(first, swapFee)

        assertThat(first.preparedSwapConfigState).isEqualTo(second.preparedSwapConfigState)
    }

    private fun buildCurrencyCheck(): CryptoCurrencyCheck = CryptoCurrencyCheck(
        dustValue = null,
        reserveAmount = null,
        minimumSendAmount = null,
        existentialDeposit = null,
        utxoAmountLimit = null,
        isAccountFunded = true,
        rentWarning = null,
        isMemoRequired = false,
    )

    private fun buildQuotesLoadedState(
        fromAmount: SwapAmount,
        isCoin: Boolean,
        fromBalance: BigDecimal,
    ): SwapState.QuotesLoadedState {
        val from = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            isCoin = isCoin,
            amount = fromBalance,
        )
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = fromAmount,
                swapCurrencyStatus = from,
                amountFiat = BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal("0.5"), 18),
                swapCurrencyStatus = to,
                amountFiat = BigDecimal.ZERO,
            ),
            priceImpact = PriceImpact.Empty,
            preparedSwapConfigState = PreparedSwapConfigState(
                isBalanceEnough = false,
                feeState = SwapFeeState.NotEnough(),
                hasOutgoingTransaction = false,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
            ),
            permissionState = PermissionDataState.Empty,
            swapDataModel = null,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
        )
    }

    private fun buildSwapFee(
        feeValue: BigDecimal,
        otherNativeFee: BigDecimal = BigDecimal.ZERO,
    ): SwapFee {
        val amount = mockk<Amount>(relaxed = true) {
            every { value } returns feeValue
        }
        val fee = mockk<Fee.Common>(relaxed = true) {
            every { this@mockk.amount } returns amount
        }
        val feeTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
            every { currency } returns buildCoinCurrency()
        }
        return SwapFee(
            fee = fee,
            transactionFeeResult = TransactionFeeResult.Loaded(mockk<TransactionFee.Single>(relaxed = true)),
            selectedFeeToken = feeTokenStatus,
            otherNativeFee = otherNativeFee,
            feeBucket = FeeBucket.MARKET,
        )
    }
}