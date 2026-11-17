package com.tangem.domain.hotwallet

import arrow.core.Either
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.QuotesRepository
import java.math.BigDecimal
import java.math.MathContext

class CheckHotWalletBalanceThresholdUseCase(
    private val hotWalletRepository: HotWalletRepository,
    private val quotesRepository: QuotesRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        totalFiatBalance: TotalFiatBalance,
        currencies: List<CryptoCurrencyStatus>,
    ): Either<Throwable, Boolean> = Either.catch {
        if (totalFiatBalance !is TotalFiatBalance.Loaded || totalFiatBalance.source != StatusSource.ACTUAL) {
            return@catch false
        }

        val quote = findConversionQuote(currencies) ?: return@catch false
        // Multiplied first: a rounded rate would put a balance of exactly $5,000 a fraction below it
        val usdBalance = totalFiatBalance.amount.multiply(quote.fiatRateUSD).divide(quote.fiatRate, USD_MATH_CONTEXT)

        val isReached = usdBalance >= THRESHOLD_USD
        if (isReached) hotWalletRepository.setBalanceThresholdReached(userWalletId, isReached = true)
        isReached
    }

    /**
     * The quote of any token priced both in the app currency and in USD converts the total, without a separate request.
     * Its rate must be the one the balance was summed with, and the most expensive token is taken: a cheap token's
     * prices are rounded to a few digits, which skews their ratio.
     */
    private suspend fun findConversionQuote(currencies: List<CryptoCurrencyStatus>): QuoteStatus.Data? {
        val balanceRates = currencies
            .mapNotNull { status ->
                val rawCurrencyId = status.currency.id.rawCurrencyId ?: return@mapNotNull null
                val fiatRate = status.value.fiatRate?.takeIf { it.signum() > 0 } ?: return@mapNotNull null
                rawCurrencyId to fiatRate
            }
            .toMap()

        if (balanceRates.isEmpty()) return null

        return quotesRepository.getMultiQuoteSyncOrNull(balanceRates.keys)
            .orEmpty()
            .mapNotNull { quote ->
                val data = quote.value as? QuoteStatus.Data ?: return@mapNotNull null
                val balanceRate = balanceRates[quote.rawCurrencyId] ?: return@mapNotNull null
                data.takeIf { it.fiatRate.compareTo(balanceRate) == 0 && it.fiatRateUSD.signum() > 0 }
            }
            .maxByOrNull(QuoteStatus.Data::fiatRate)
    }

    companion object {
        val THRESHOLD_USD: BigDecimal = BigDecimal(5_000)

        private val USD_MATH_CONTEXT = MathContext.DECIMAL128
    }
}