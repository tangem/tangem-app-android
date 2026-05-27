package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.ui.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Tests for the Tangem Pay early-exit branch in [SwapInteractorImpl].
 *
 * When the from-currency belongs to a [Account.Payment] account the fee must
 * never be included in the swap amount ([IncludeFeeInAmountInternal.Excluded]).
 *
 * The private [SwapInteractorImpl.getIncludeFeeInAmountInternal] function is exercised
 * through the public [SwapInteractorImpl.applySwapFee] entry point (CEX provider path),
 * which calls [computeBalanceStatus] → [getIncludeFeeInAmountInternal].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("SwapInteractorImpl — Tangem Pay (Payment account) fee-inclusion behaviour")
internal class SwapInteractorImplTangemPayTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val lastReducedBalanceBy = BigDecimal.ZERO

    @BeforeEach
    fun setup() {
        // Shared stubs required by computeBalanceStatus / manageWarnings / manageTransactionValidationWarnings
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
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns buildCoinCurrency()
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
    }

    // -------------------------------------------------------------------------
    // Payment account — fee always Excluded
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Payment account (Tangem Pay withdrawal)")
    inner class PaymentAccountBranch {

        @Test
        @DisplayName("should produce Sufficient and not FeeAdjustedAmount when Payment account token amount within balance")
        fun `should produce Sufficient when Payment account token swap and amount within balance`() = runTest {
            // Token swap: balance=1, amount=0.95, fee=0.1 (amount + fee > balance).
            // On a CryptoPortfolio account with a same-currency token fee this triggers FeeAdjustedAmount.
            // On a Payment account the early-exit returns Excluded, so computeBalanceStatus falls through
            // to isBalanceEnough (token: checks balance >= amount only → true) → Sufficient.
            val state = buildCexQuotesLoadedState(
                fromAmount = SwapAmount(BigDecimal("0.95"), 18),
                isCoin = false,
                fromBalance = BigDecimal("1"),
                account = Account.Payment(userWalletId),
            )
            val swapFee = buildTestSwapFee(feeValue = BigDecimal("0.001"))

            val patched = sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

            assertThat(patched.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.Sufficient::class.java)
        }

        @Test
        @DisplayName("should produce Sufficient even when from-token and fee-token ids match (same-currency token path bypassed)")
        fun `should produce Sufficient when Payment account and same-currency token fee selected`() = runTest {
            // With a CryptoPortfolio account this scenario (same token for fee and swap) would trigger
            // the IncludeFeeInAmountInternal.Included / BalanceNotEnough paths.
            // Payment account must short-circuit before reaching that logic.
            val state = buildCexQuotesLoadedState(
                fromAmount = SwapAmount(BigDecimal("1"), 18),
                isCoin = false,
                fromBalance = BigDecimal("1"),
                account = Account.Payment(userWalletId),
            )
            // Build a fee token that shares the same currency id as the from-token — triggers same-currency path
            // on non-Payment accounts.
            val fromCurrencyStatus = state.fromTokenInfo.swapCurrencyStatus.status
            val swapFee = buildTestSwapFeeWithToken(
                feeValue = BigDecimal("0.5"),
                selectedFeeToken = fromCurrencyStatus,
            )

            val patched = sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

            // Must be Sufficient, not FeeAdjustedAmount or InsufficientFee
            assertThat(patched.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.Sufficient::class.java)
        }

        @Test
        @DisplayName("should produce InsufficientAmount when Payment account and amount exceeds balance")
        fun `should produce InsufficientAmount when Payment account and amount exceeds from-balance`() = runTest {
            // Even on a Payment account the basic amount-vs-balance check must still apply.
            coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")

            val state = buildCexQuotesLoadedState(
                fromAmount = SwapAmount(BigDecimal("5"), 18),
                isCoin = true,
                fromBalance = BigDecimal("1"),
                account = Account.Payment(userWalletId),
            )
            val swapFee = buildTestSwapFee(feeValue = BigDecimal("0.001"))

            val patched = sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

            assertThat(patched.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.InsufficientAmount::class.java)
        }
    }

    // -------------------------------------------------------------------------
    // Non-Payment account — existing native-fee logic preserved
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("CryptoPortfolio account (existing behaviour preserved)")
    inner class CryptoPortfolioAccountBranch {

        @Test
        @DisplayName("should produce Sufficient when CryptoPortfolio account and native balance covers fee")
        fun `should produce Sufficient when CryptoPortfolio account and native balance covers fee`() = runTest {
            coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")

            val state = buildCexQuotesLoadedState(
                fromAmount = SwapAmount(BigDecimal("1"), 18),
                isCoin = true,
                fromBalance = BigDecimal("10"),
                account = Account.CryptoPortfolio.createMainAccount(userWalletId),
            )
            val swapFee = buildTestSwapFee(feeValue = BigDecimal("0.001"))

            val patched = sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

            assertThat(patched.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.Sufficient::class.java)
        }

        @Test
        @DisplayName("should produce InsufficientFee when CryptoPortfolio account and native balance below fee")
        fun `should produce InsufficientFee when CryptoPortfolio and native balance below fee`() = runTest {
            coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("0.0001")

            val state = buildCexQuotesLoadedState(
                fromAmount = SwapAmount(BigDecimal("1"), 18),
                isCoin = false, // token → fee paid from native
                fromBalance = BigDecimal("10"),
                account = Account.CryptoPortfolio.createMainAccount(userWalletId),
            )
            val swapFee = buildTestSwapFee(feeValue = BigDecimal("0.01"))

            val patched = sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

            assertThat(patched.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
        }

        @Test
        @DisplayName("should produce FeeAdjustedAmount when CryptoPortfolio account, same-currency token fee, and amount fills balance")
        fun `should produce FeeAdjustedAmount when CryptoPortfolio and same-currency token fee squeezes amount`() =
            runTest {
                // same-token fee path: amount fills the balance but amount + fee > balance → FeeAdjustedAmount
                val fromBalance = BigDecimal("1")
                val feeValue = BigDecimal("0.1")
                val amount = BigDecimal("0.95") // 0.95 + 0.1 = 1.05 > 1 → triggers Included

                val state = buildCexQuotesLoadedState(
                    fromAmount = SwapAmount(amount, 18),
                    isCoin = false,
                    fromBalance = fromBalance,
                    account = Account.CryptoPortfolio.createMainAccount(userWalletId),
                )
                val fromCurrencyStatus = state.fromTokenInfo.swapCurrencyStatus.status
                val swapFee = buildTestSwapFeeWithToken(
                    feeValue = feeValue,
                    selectedFeeToken = fromCurrencyStatus,
                )

                val patched = sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

                assertThat(patched.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.FeeAdjustedAmount::class.java)
            }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    /**
     * Builds a [SwapState.QuotesLoadedState] with a CEX provider so that [applySwapFee] routes
     * through [computeBalanceStatus] → [getIncludeFeeInAmountInternal].
     *
     * The [account] parameter is the real domain [Account] instance to put on [SwapCurrencyStatus].
     */
    private fun buildCexQuotesLoadedState(
        fromAmount: SwapAmount,
        isCoin: Boolean,
        fromBalance: BigDecimal,
        account: Account,
    ): SwapState.QuotesLoadedState {
        val from = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            isCoin = isCoin,
            amount = fromBalance,
        ).copy(account = account)
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
                balanceStatus = SwapBalanceStatus.Pending,
                hasOutgoingTransaction = false,
            ),
            permissionState = PermissionDataState.Empty,
            swapDataModel = null,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildSwapProvider(ExchangeProviderType.CEX),
        )
    }

    private fun buildTestSwapFee(
        feeValue: BigDecimal,
        otherNativeFee: BigDecimal = BigDecimal.ZERO,
    ): SwapFee {
        val feeAmount = mockk<Amount>(relaxed = true) {
            every { value } returns feeValue
        }
        val fee = mockk<Fee.Common>(relaxed = true) {
            every { this@mockk.amount } returns feeAmount
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

    /**
     * Builds a [SwapFee] whose [SwapFee.selectedFeeToken] is the given [CryptoCurrencyStatus].
     * This triggers the same-currency-token path in [getIncludeFeeInAmountInternal] for
     * [Account.CryptoPortfolio] accounts.
     */
    private fun buildTestSwapFeeWithToken(
        feeValue: BigDecimal,
        selectedFeeToken: CryptoCurrencyStatus,
    ): SwapFee {
        val feeAmount = mockk<Amount>(relaxed = true) {
            every { value } returns feeValue
        }
        val fee = mockk<Fee.Common>(relaxed = true) {
            every { this@mockk.amount } returns feeAmount
        }
        return SwapFee(
            fee = fee,
            transactionFeeResult = TransactionFeeResult.Loaded(mockk<TransactionFee.Single>(relaxed = true)),
            selectedFeeToken = selectedFeeToken,
            otherNativeFee = BigDecimal.ZERO,
            feeBucket = FeeBucket.MARKET,
        )
    }
}