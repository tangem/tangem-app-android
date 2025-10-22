package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.yield.supply.YieldSupplyRepository
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Calculates max allowed network fee for Yield Supply enter transaction expressed in token units.
 *
 * Uses YieldMarketToken.maxFeeNative (native coin units) and converts it to token units with the
 * same conversion logic as [YieldSupplyGetCurrentFeeUseCase]: based on fiat rate ratio
 * (nativeFiatRate / tokenFiatRate).
 */
class YieldSupplyGetMaxFeeUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val quotesRepository: QuotesRepository,
    private val currenciesRepository: CurrenciesRepository,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<Throwable, BigDecimal> = catch {
        val token = cryptoCurrencyStatus.currency as? CryptoCurrency.Token
            ?: error("CryptoCurrency must be token for max fee calculation")

        val fiatRate = cryptoCurrencyStatus.value.fiatRate ?: error("Fiat rate is missing")
        require(fiatRate > BigDecimal.ZERO) { "Fiat rate for token must be > 0" }

        val nativeCryptoCurrency = currenciesRepository.getNetworkCoin(
            userWalletId = userWallet.walletId,
            networkId = cryptoCurrencyStatus.currency.network.id,
            derivationPath = cryptoCurrencyStatus.currency.network.derivationPath,
        )

        val quotes =
            quotesRepository.getMultiQuoteSyncOrNull(setOfNotNull(nativeCryptoCurrency.id.rawCurrencyId))
                ?: error("Quotes for native coin are unavailable")

        val quotesStatus = quotes.firstOrNull() ?: error("Empty quotes list for native coin")

        val nativeFiatRate = (quotesStatus.value as? QuoteStatus.Data)?.fiatRate
            ?: error("Native fiat rate is missing")
        require(nativeFiatRate > BigDecimal.ZERO) { "Native fiat rate must be > 0" }

        val cachedMarketToken = yieldSupplyRepository.getCachedMarkets().orEmpty()
            .firstOrNull { it.yieldSupplyKey == token.yieldSupplyKey() }
        val marketToken = cachedMarketToken ?: yieldSupplyRepository.getTokenStatus(token)
        val maxFeeNative = marketToken.maxFeeNative.toBigDecimal()

        val rateRatio = nativeFiatRate.divide(
            fiatRate,
            cryptoCurrencyStatus.currency.decimals,
            RoundingMode.HALF_UP,
        )

        val tokenValue = rateRatio.multiply(maxFeeNative)

        tokenValue.stripTrailingZeros()
    }
}