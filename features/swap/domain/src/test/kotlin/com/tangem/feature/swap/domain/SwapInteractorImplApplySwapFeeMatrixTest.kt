package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Matrix-style coverage for [SwapInteractorImpl.applySwapFee] across all combinations of:
 *   - Provider type: DEX / DEX_BRIDGE / CEX
 *   - FeePaidCurrency: Coin / Token / SameCurrency / FeeResource
 *   - from-token shape: Coin vs Token
 *
 * KEY INVARIANT ([REDACTED_TASK_KEY]):
 *   "For DEX, fee cannot be subtracted from the swap amount."
 *   → When amount + fee > balance, DEX must return InsufficientFee, never FeeAdjustedAmount.
 *   → CEX returns FeeAdjustedAmount in the same scenario.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplApplySwapFeeMatrixTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val lastReducedBalanceBy = BigDecimal.ZERO

    @BeforeEach
    fun setup() {
        // Default stubs that keep all tests alive unless they override:
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
    }

    // =========================================================================
    // Section A: DEX/CEX asymmetry — the KEY INVARIANT
    // =========================================================================

    @Nested
    inner class `DEX vs CEX asymmetry - fee-cannot-deduct invariant` {

        /**
         * GIVEN  ExchangeProviderType.DEX
         *        fromToken is Coin, status.value.amount = 1.1 ETH (isBalanceEnough passes: 1.1 >= 1.0+0.01)
         *        FeePaidCurrency.Coin, walletManagersFacade.getNativeTokenBalance = 1.0 ETH
         *        amount = 1.0 ETH, fee = 0.01 ETH
         * WHEN   applySwapFee runs
         * THEN   balanceStatus == InsufficientFee (NOT FeeAdjustedAmount)
         *
         * The DEX invariant: DEX never reduces the amount to include fee.
         * computeBalanceStatus for DEX/DEX_BRIDGE skips getIncludeFeeInAmountInternal entirely,
         * then falls to getFeeBalanceState. With nativeBalance=1.0 and amount=1.0:
         *   balanceToCheck = nativeBalance(1.0) - amount(1.0) = 0 ≤ fee(0.01) → InsufficientFee.
         *
         * NOTE: fromBalance (status.value.amount) must be > amount+fee so isBalanceEnough()
         * passes and we reach getFeeBalanceState. The walletManagersFacade balance is what
         * triggers the InsufficientFee via getFeeBalanceState for the coin case.
         */
        @Test
        fun `applySwapFee DEX with Coin fee — amount+fee greater than balance returns InsufficientFee (cannot deduct on DEX)`() =
            runTest {
                coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
                // Native balance (for fee deduction check) = 1.0 ETH.
                // After subtracting amount (1.0 ETH), 0 remains which is < fee (0.01 ETH).
                coEvery {
                    walletManagersFacade.getNativeTokenBalance(any(), any(), any())
                } returns BigDecimal("1.0")

                // fromBalance must be larger than amount+fee so isBalanceEnough() passes.
                // status.value.amount = 1.1 ETH: 1.1 >= 1.0+0.01=1.01 → isBalanceEnough=true
                val state = buildQuotesLoadedState(
                    providerType = ExchangeProviderType.DEX,
                    fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                    isCoin = true,
                    fromBalance = BigDecimal("1.1"),
                )
                val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.01"))

                val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

                // DEX must NOT return FeeAdjustedAmount — it must return InsufficientFee
                assertThat(result.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
            }

        /**
         * CEX twin: same native-balance scenario → FeeAdjustedAmount (CEX can include fee in amount).
         *
         * GIVEN  ExchangeProviderType.CEX
         *        fromToken is Coin, status.value.amount = 1.1 ETH, amount = 1.0 ETH, fee = 0.01 ETH
         *        walletManagersFacade.getNativeTokenBalance = 1.0 ETH
         * WHEN   applySwapFee runs
         * THEN   balanceStatus == FeeAdjustedAmount (CEX auto-reduces amount)
         *
         * For CEX, getIncludeFeeInAmountInternal fires:
         *   nativeBalance = 1.0, amount = 1.0, amountWithFee = 1.01 > 1.0 = nativeBalance
         *   AND fee(0.01) < amount(1.0) → Included → FeeAdjustedAmount.
         */
        @Test
        fun `applySwapFee CEX with Coin fee — amount+fee greater than nativeBalance returns FeeAdjustedAmount (can deduct on CEX)`() =
            runTest {
                coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
                // Native balance for fee calculation path
                coEvery {
                    walletManagersFacade.getNativeTokenBalance(any(), any(), any())
                } returns BigDecimal("1.0")

                // fromBalance (status.value.amount) must pass isBalanceEnough for CEX too
                val state = buildQuotesLoadedState(
                    providerType = ExchangeProviderType.CEX,
                    fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                    isCoin = true,
                    fromBalance = BigDecimal("1.1"),
                )
                val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.01"))

                val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

                assertThat(result.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.FeeAdjustedAmount::class.java)
            }

        /**
         * DEX_BRIDGE mirrors DEX: same nativeBalance scenario returns InsufficientFee.
         */
        @Test
        fun `applySwapFee DEX_BRIDGE with Coin fee — amount+fee greater than balance returns InsufficientFee`() =
            runTest {
                coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
                coEvery {
                    walletManagersFacade.getNativeTokenBalance(any(), any(), any())
                } returns BigDecimal("1.0")

                val state = buildQuotesLoadedState(
                    providerType = ExchangeProviderType.DEX_BRIDGE,
                    fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                    isCoin = true,
                    fromBalance = BigDecimal("1.1"),
                )
                val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.01"))

                val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

                assertThat(result.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
            }

        /**
         * DEX happy path: balance comfortably covers both amount and fee.
         * Must return Sufficient, not FeeAdjustedAmount.
         */
        @Test
        fun `applySwapFee DEX with Coin fee — balance covers amount+fee returns Sufficient`() = runTest {
            coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
            coEvery {
                walletManagersFacade.getNativeTokenBalance(any(), any(), any())
            } returns BigDecimal("2.0")

            val state = buildQuotesLoadedState(
                providerType = ExchangeProviderType.DEX,
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                isCoin = true,
                fromBalance = BigDecimal("2.0"),
            )
            val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.01"))

            val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

            assertThat(result.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.Sufficient::class.java)
        }
    }

    // =========================================================================
    // Section B: FeePaidCurrency.Token (gasless-token) paths
    // =========================================================================

    @Nested
    inner class `FeePaidCurrency Token paths` {

        /**
         * FeePaidCurrency.Token with sufficient token balance → Sufficient.
         * The from-token is a Token on ETH; fee is paid from a different gasless token
         * whose balance (5.0) comfortably exceeds the fee (0.001).
         */
        @Test
        fun `applySwapFee — FeePaidCurrency Token — sufficient gasless-token balance returns Sufficient`() =
            runTest {
                val gaslessTokenId = mockk<CryptoCurrency.ID>(relaxed = true)
                val gaslessToken = mockk<CryptoCurrency.Token>(relaxed = true) {
                    every { id } returns gaslessTokenId
                }
                val gaslessTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
                    every { currency } returns gaslessToken
                    every { value.amount } returns BigDecimal("5.0")
                }

                // FeePaidCurrency.Token with balance=5.0 > fee=0.001 → Enough
                coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Token(
                    tokenId = gaslessTokenId,
                    name = "GasToken",
                    symbol = "GAS",
                    contractAddress = "0xGasTokenAddress",
                    balance = BigDecimal("5.0"),
                )

                val fromId = mockk<CryptoCurrency.ID>(relaxed = true)
                val state = buildQuotesLoadedStateWithTokenFrom(
                    providerType = ExchangeProviderType.DEX,
                    fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                    fromBalance = BigDecimal("10.0"),
                    fromTokenId = fromId,
                )
                // selectedFeeToken is the gasless token (different from fromToken)
                val fee = buildSwapFeeWithExplicitToken(
                    feeValue = BigDecimal("0.001"),
                    tokenStatus = gaslessTokenStatus,
                    tokenId = gaslessTokenId,
                )

                val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

                assertThat(result.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.Sufficient::class.java)
            }

        /**
         * FeePaidCurrency.Token with insufficient token balance → InsufficientFee.
         * The gasless token balance (0.0005) is below the fee (0.001).
         */
        @Test
        fun `applySwapFee — FeePaidCurrency Token — insufficient gasless-token balance returns InsufficientFee`() =
            runTest {
                val gaslessTokenId = mockk<CryptoCurrency.ID>(relaxed = true)
                val gaslessToken = mockk<CryptoCurrency.Token>(relaxed = true) {
                    every { id } returns gaslessTokenId
                    every { name } returns "GasToken"
                    every { symbol } returns "GAS"
                }
                val gaslessTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
                    every { currency } returns gaslessToken
                    every { value.amount } returns BigDecimal("0.0005")
                }

                // FeePaidCurrency.Token with balance=0.0005 < fee=0.001 → NotEnough
                coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Token(
                    tokenId = gaslessTokenId,
                    name = "GasToken",
                    symbol = "GAS",
                    contractAddress = "0xGasTokenAddress",
                    balance = BigDecimal("0.0005"),
                )

                val fromId = mockk<CryptoCurrency.ID>(relaxed = true)
                val state = buildQuotesLoadedStateWithTokenFrom(
                    providerType = ExchangeProviderType.DEX,
                    fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                    fromBalance = BigDecimal("10.0"),
                    fromTokenId = fromId,
                )
                val fee = buildSwapFeeWithExplicitToken(
                    feeValue = BigDecimal("0.001"),
                    tokenStatus = gaslessTokenStatus,
                    tokenId = gaslessTokenId,
                )

                val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

                assertThat(result.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
            }

        /**
         * FeePaidCurrency.Token — verifies the fee currency name/symbol propagate into
         * the InsufficientFee status so the UI can show "Not enough GAS for fee".
         */
        @Test
        fun `applySwapFee — FeePaidCurrency Token — InsufficientFee carries token name and symbol`() = runTest {
            val gaslessTokenId = mockk<CryptoCurrency.ID>(relaxed = true)
            val gaslessToken = mockk<CryptoCurrency.Token>(relaxed = true) {
                every { id } returns gaslessTokenId
                every { name } returns "GasToken"
                every { symbol } returns "GAS"
            }
            val gaslessTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
                every { currency } returns gaslessToken
                every { value.amount } returns BigDecimal("0.0005")
            }

            coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Token(
                tokenId = gaslessTokenId,
                name = "GasToken",
                symbol = "GAS",
                contractAddress = "0xGasTokenAddress",
                balance = BigDecimal("0.0005"),
            )

            val fromId = mockk<CryptoCurrency.ID>(relaxed = true)
            val state = buildQuotesLoadedStateWithTokenFrom(
                providerType = ExchangeProviderType.CEX,
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                fromBalance = BigDecimal("10.0"),
                fromTokenId = fromId,
            )
            val fee = buildSwapFeeWithExplicitToken(
                feeValue = BigDecimal("0.001"),
                tokenStatus = gaslessTokenStatus,
                tokenId = gaslessTokenId,
            )

            val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

            val status = result.preparedSwapConfigState.balanceStatus
            assertThat(status).isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
            val insufficientFee = status as SwapBalanceStatus.InsufficientFee
            assertThat(insufficientFee.feeCurrencySymbol).isEqualTo("GAS")
            assertThat(insufficientFee.feeCurrencyName).isEqualTo("GasToken")
        }
    }

    // =========================================================================
    // Section C: FeePaidCurrency.SameCurrency paths
    // =========================================================================

    @Nested
    inner class `FeePaidCurrency SameCurrency paths` {

        /**
         * FeePaidCurrency.SameCurrency on CEX: fromToken is a Token, fee is paid in the same
         * token, balance comfortably covers amount + fee → Sufficient.
         * (This is the Cardano-style path where the fee currency == the send currency.)
         */
        @Test
        fun `applySwapFee CEX — FeePaidCurrency SameCurrency — sufficient balance returns Sufficient`() = runTest {
            coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.SameCurrency

            val state = buildQuotesLoadedState(
                providerType = ExchangeProviderType.CEX,
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                isCoin = false,
                fromBalance = BigDecimal("10.0"),
            )
            // Fee is low enough: balance(10) - amount(1) = 9 > fee(0.001)
            val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.001"))

            val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

            assertThat(result.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.Sufficient::class.java)
        }

        /**
         * FeePaidCurrency.SameCurrency on DEX: balance - amount just covers the fee → Sufficient.
         * (DEX doesn't invoke getIncludeFeeInAmountInternal so it falls through to getFeeBalanceState.)
         */
        @Test
        fun `applySwapFee DEX — FeePaidCurrency SameCurrency — balance minus amount covers fee returns Sufficient`() =
            runTest {
                coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.SameCurrency

                // balance=10, amount=1, fee=0.5 → balance-amount=9 > fee=0.5 → Sufficient
                val state = buildQuotesLoadedState(
                    providerType = ExchangeProviderType.DEX,
                    fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                    isCoin = false,
                    fromBalance = BigDecimal("10.0"),
                )
                val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.5"))

                val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

                assertThat(result.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.Sufficient::class.java)
            }

        /**
         * FeePaidCurrency.SameCurrency: balance - amount is less than fee → InsufficientFee.
         */
        @Test
        fun `applySwapFee — FeePaidCurrency SameCurrency — balance minus amount below fee returns InsufficientFee`() =
            runTest {
                coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.SameCurrency

                // balance=1.0, amount=1.0, fee=0.001 → balance-amount=0 ≤ fee → NotEnough
                val state = buildQuotesLoadedState(
                    providerType = ExchangeProviderType.DEX,
                    fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                    isCoin = false,
                    fromBalance = BigDecimal("1.0"),
                )
                val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.001"))

                val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

                assertThat(result.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
            }
    }

    // =========================================================================
    // Section D: FeePaidCurrency.FeeResource paths
    // =========================================================================

    @Nested
    inner class `FeePaidCurrency FeeResource paths` {

        /**
         * FeeResource, isFeeResourceEnough = true → Sufficient (happy path — already tested
         * in SwapInteractorImplApplySwapFeeTest but verified here for clarity).
         */
        @Test
        fun `applySwapFee — FeeResource — isFeeResourceEnough true returns Sufficient`() = runTest {
            coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns
                FeePaidCurrency.FeeResource(currency = "MANA")
            coEvery { currencyChecksRepository.checkIfFeeResourceEnough(any(), any(), any()) } returns true

            val state = buildQuotesLoadedState(
                providerType = ExchangeProviderType.DEX,
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                isCoin = true,
                fromBalance = BigDecimal("10.0"),
            )
            val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.001"))

            val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

            assertThat(result.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.Sufficient::class.java)
        }

        /**
         * FeeResource, isFeeResourceEnough = false → InsufficientFee.
         * This is the MISSING unhappy path that was requested in the audit.
         */
        @Test
        fun `applySwapFee — FeeResource — isFeeResourceEnough false returns InsufficientFee`() = runTest {
            coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns
                FeePaidCurrency.FeeResource(currency = "MANA")
            coEvery { currencyChecksRepository.checkIfFeeResourceEnough(any(), any(), any()) } returns false

            val state = buildQuotesLoadedState(
                providerType = ExchangeProviderType.DEX,
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                isCoin = true,
                fromBalance = BigDecimal("10.0"),
            )
            val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.001"))

            val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

            assertThat(result.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
        }

        /**
         * FeeResource on CEX: isFeeResourceEnough = false → InsufficientFee even for CEX,
         * because CEX's FeeAdjustedAmount path is only taken for native-coin fee deduction,
         * not for fee resources.
         */
        @Test
        fun `applySwapFee CEX — FeeResource — isFeeResourceEnough false returns InsufficientFee`() = runTest {
            coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns
                FeePaidCurrency.FeeResource(currency = "MANA")
            coEvery { currencyChecksRepository.checkIfFeeResourceEnough(any(), any(), any()) } returns false

            val state = buildQuotesLoadedState(
                providerType = ExchangeProviderType.CEX,
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                isCoin = true,
                fromBalance = BigDecimal("10.0"),
            )
            val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.001"))

            val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

            assertThat(result.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
        }
    }

    // =========================================================================
    // Section E: FeePaidCurrency.Coin — from-token is Token (fee paid separately)
    // =========================================================================

    @Nested
    inner class `FeePaidCurrency Coin - from is Token` {

        /**
         * From-token is an ERC-20 Token, FeePaidCurrency.Coin (ETH pays the gas).
         * Native balance comfortably covers the fee → Sufficient.
         * No amount+fee concern because the fee currency (ETH) != from-token (USDC).
         */
        @Test
        fun `applySwapFee DEX — Coin fee — from is Token — native balance covers fee returns Sufficient`() =
            runTest {
                coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
                coEvery {
                    walletManagersFacade.getNativeTokenBalance(any(), any(), any())
                } returns BigDecimal("0.5")

                val state = buildQuotesLoadedState(
                    providerType = ExchangeProviderType.DEX,
                    fromAmount = SwapAmount(BigDecimal("100.0"), 6),  // 100 USDC
                    isCoin = false,
                    fromBalance = BigDecimal("200.0"),
                )
                val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.001"))

                val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

                assertThat(result.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.Sufficient::class.java)
            }

        /**
         * From-token is an ERC-20 Token, FeePaidCurrency.Coin.
         * Native balance (0.0001 ETH) is less than fee (0.001 ETH) → InsufficientFee.
         * The from-token balance (200 USDC) is irrelevant for the fee check.
         */
        @Test
        fun `applySwapFee DEX — Coin fee — from is Token — native balance below fee returns InsufficientFee`() =
            runTest {
                coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
                coEvery {
                    walletManagersFacade.getNativeTokenBalance(any(), any(), any())
                } returns BigDecimal("0.0001")

                val state = buildQuotesLoadedState(
                    providerType = ExchangeProviderType.DEX,
                    fromAmount = SwapAmount(BigDecimal("100.0"), 6),
                    isCoin = false,
                    fromBalance = BigDecimal("200.0"),
                )
                val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.001"))

                val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

                assertThat(result.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
            }

        /**
         * CEX + from is Token + FeePaidCurrency.Coin.
         * Amount (100) ≤ token balance (200). Native balance (0.0001) < fee (0.001).
         *
         * For CEX the getIncludeFeeInAmountInternal path runs. Because feePaidCurrency is NOT
         * a same-currency-token (fromToken != feeToken), it falls to getIncludeFeeInAmountForNative
         * which detects fromCurrency is CryptoCurrency.Token, then checks nativeBalance >= fee.
         * 0.0001 < 0.001 → BalanceNotEnough → falls through to getFeeBalanceState → InsufficientFee.
         *
         * Note: CEX does NOT return FeeAdjustedAmount when from-token is a Token because
         * feeAdjustedAmount only applies to the native-coin-from path in getIncludeFeeAmountForCoinFee.
         */
        @Test
        fun `applySwapFee CEX — Coin fee — from is Token — native balance below fee returns InsufficientFee`() =
            runTest {
                coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
                coEvery {
                    walletManagersFacade.getNativeTokenBalance(any(), any(), any())
                } returns BigDecimal("0.0001")

                val state = buildQuotesLoadedState(
                    providerType = ExchangeProviderType.CEX,
                    fromAmount = SwapAmount(BigDecimal("100.0"), 6),
                    isCoin = false,
                    fromBalance = BigDecimal("200.0"),
                )
                val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.001"))

                val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

                assertThat(result.preparedSwapConfigState.balanceStatus)
                    .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
            }
    }

    // =========================================================================
    // Section F: Amount-alone insufficient (InsufficientAmount)
    // =========================================================================

    @Nested
    inner class `InsufficientAmount paths` {

        /**
         * DEX + fromToken is Coin + amount > balance → InsufficientAmount regardless of fee.
         * isBalanceEnough() checks amount + fee for Coin, so balance < amount alone → InsufficientAmount.
         */
        @Test
        fun `applySwapFee DEX — Coin — amount exceeds balance returns InsufficientAmount`() = runTest {
            coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
            coEvery {
                walletManagersFacade.getNativeTokenBalance(any(), any(), any())
            } returns BigDecimal("0.5")

            // Amount = 1.0 but native balance (used for coins) = 0.5
            val state = buildQuotesLoadedState(
                providerType = ExchangeProviderType.DEX,
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                isCoin = true,
                fromBalance = BigDecimal("0.5"), // status.value.amount used by getTokenBalance
            )
            val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.001"))

            val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

            assertThat(result.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.InsufficientAmount::class.java)
        }

        /**
         * From-token is an ERC-20 Token; amount > token balance → InsufficientAmount.
         * The native balance is irrelevant for the amount check when from is Token
         * (FeePaidCurrency.Coin → token balance check only for isBalanceEnough).
         */
        @Test
        fun `applySwapFee — Token from — amount exceeds token balance returns InsufficientAmount`() = runTest {
            coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
            coEvery {
                walletManagersFacade.getNativeTokenBalance(any(), any(), any())
            } returns BigDecimal("10.0") // plenty of ETH for fee

            // amount = 100 USDC but fromBalance = 50 USDC
            val state = buildQuotesLoadedState(
                providerType = ExchangeProviderType.DEX,
                fromAmount = SwapAmount(BigDecimal("100.0"), 6),
                isCoin = false,
                fromBalance = BigDecimal("50.0"),
            )
            val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.001"))

            val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

            assertThat(result.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.InsufficientAmount::class.java)
        }
    }

    // =========================================================================
    // Section G: FeeAdjustedAmount carries the correct adjusted value
    // =========================================================================

    @Nested
    inner class `FeeAdjustedAmount value correctness` {

        /**
         * CEX + Coin from + amount+fee just barely doesn't fit.
         * The adjusted amount must be nativeBalance - fee (not zero, not the original amount).
         *
         * Scenario:
         *   status.value.amount (fromBalance for isBalanceEnough) = 1.1
         *   walletManagersFacade nativeBalance = 1.0
         *   amount = 0.999, fee = 0.005
         *
         * isBalanceEnough: 1.1 >= 0.999 + 0.005 = 1.004 → TRUE
         * getIncludeFeeAmountForCoinFee:
         *   nativeBalance = 1.0
         *   amount(0.999) ≤ nativeBalance(1.0) ✓
         *   amountWithFee(1.004) > nativeBalance(1.0) ✓
         *   fee(0.005) < amount(0.999) ✓
         *   → Included: adjustedAmount = nativeBalance(1.0) - fee(0.005) = 0.995
         */
        @Test
        fun `applySwapFee CEX — FeeAdjustedAmount — adjustedAmount equals nativeBalance minus fee`() = runTest {
            coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
            coEvery {
                walletManagersFacade.getNativeTokenBalance(any(), any(), any())
            } returns BigDecimal("1.0")

            val state = buildQuotesLoadedState(
                providerType = ExchangeProviderType.CEX,
                fromAmount = SwapAmount(BigDecimal("0.999"), 18),
                isCoin = true,
                fromBalance = BigDecimal("1.1"),  // larger than amount+fee so isBalanceEnough passes
            )
            val fee = buildSwapFeeWithCoinToken(feeValue = BigDecimal("0.005"))

            val result = sut.applySwapFee(state, fee, lastReducedBalanceBy)

            val status = result.preparedSwapConfigState.balanceStatus
            assertThat(status).isInstanceOf(SwapBalanceStatus.FeeAdjustedAmount::class.java)
            val adjusted = status as SwapBalanceStatus.FeeAdjustedAmount
            // adjustedAmount = nativeBalance(1.0) - fee(0.005) = 0.995
            assertThat(adjusted.adjustedAmount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.995"))
        }
    }

    // =========================================================================
    // Helpers — local builders (scope-specific, private to this test class)
    // =========================================================================

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
     * Builds a QuotesLoadedState with the specified provider and a [CryptoCurrency.Coin] from-token
     * (when [isCoin] = true) or a [CryptoCurrency.Token] from-token (when [isCoin] = false).
     */
    private fun buildQuotesLoadedState(
        providerType: ExchangeProviderType,
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
                balanceStatus = SwapBalanceStatus.Pending,
                hasOutgoingTransaction = false,
            ),
            permissionState = PermissionDataState.Empty,
            swapDataModel = null,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildSwapProvider(providerType),
        )
    }

    /**
     * Like [buildQuotesLoadedState] but creates a Token from-currency with the given [fromTokenId].
     */
    private fun buildQuotesLoadedStateWithTokenFrom(
        providerType: ExchangeProviderType,
        fromAmount: SwapAmount,
        fromBalance: BigDecimal,
        fromTokenId: CryptoCurrency.ID,
    ): SwapState.QuotesLoadedState {
        val from = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            isCoin = false,
            amount = fromBalance,
            contractAddress = "0xFromTokenAddress",
        )
        // Rewire the id on the currency mock to be the distinct fromTokenId
        every { from.status.currency.id } returns fromTokenId

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
            swapProvider = buildSwapProvider(providerType),
        )
    }

    /**
     * Builds a [com.tangem.feature.swap.domain.models.ui.SwapFee] where the [selectedFeeToken]
     * holds a [CryptoCurrency.Coin] — the normal native-coin fee scenario.
     */
    private fun buildSwapFeeWithCoinToken(
        feeValue: BigDecimal,
        otherNativeFee: BigDecimal = BigDecimal.ZERO,
    ): com.tangem.feature.swap.domain.models.ui.SwapFee {
        val amount = mockk<Amount>(relaxed = true) {
            every { value } returns feeValue
        }
        val fee = mockk<Fee.Common>(relaxed = true) {
            every { this@mockk.amount } returns amount
        }
        val coinCurrency = mockk<CryptoCurrency.Coin>(relaxed = true)
        val feeTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
            every { currency } returns coinCurrency
        }
        return com.tangem.feature.swap.domain.models.ui.SwapFee(
            fee = fee,
            transactionFeeResult = TransactionFeeResult.Loaded(mockk<TransactionFee.Single>(relaxed = true)),
            selectedFeeToken = feeTokenStatus,
            otherNativeFee = otherNativeFee,
            feeBucket = FeeBucket.MARKET,
        )
    }

    /**
     * Builds a [com.tangem.feature.swap.domain.models.ui.SwapFee] where [selectedFeeToken]
     * holds an explicit [CryptoCurrency.Token] — the gasless-token fee scenario.
     * The [tokenId] must match the one used in the gasless token mock.
     */
    private fun buildSwapFeeWithExplicitToken(
        feeValue: BigDecimal,
        tokenStatus: CryptoCurrencyStatus,
        tokenId: CryptoCurrency.ID,
    ): com.tangem.feature.swap.domain.models.ui.SwapFee {
        val amount = mockk<Amount>(relaxed = true) {
            every { value } returns feeValue
        }
        val fee = mockk<Fee.Common>(relaxed = true) {
            every { this@mockk.amount } returns amount
        }
        return com.tangem.feature.swap.domain.models.ui.SwapFee(
            fee = fee,
            transactionFeeResult = TransactionFeeResult.Loaded(mockk<TransactionFee.Single>(relaxed = true)),
            selectedFeeToken = tokenStatus,
            otherNativeFee = BigDecimal.ZERO,
            feeBucket = FeeBucket.MARKET,
        )
    }
}