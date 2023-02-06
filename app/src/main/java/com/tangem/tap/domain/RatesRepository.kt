package com.tangem.tap.domain

import com.tangem.common.services.Result
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.ThrottlerWithValues
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class RatesRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) {
    private val throttler = ThrottlerWithValues<Currency, Result<BigDecimal>?>(60000)

    suspend fun loadFiatRate(currencyId: String, coinsList: List<Currency>): Result<RatesResult> =
        withContext(dispatchers.io) {
            // get and submit previous result of equivalents.
            val throttledResult = coinsList.filter { throttler.isStillThrottled(it) }.map {
                Pair(it, throttler.geValue(it))
            }

            val currenciesToUpdate = coinsList.filter { !throttler.isStillThrottled(it) }
            val coinIds = currenciesToUpdate.mapNotNull { it.coinId }.distinct()
            if (coinIds.isEmpty()) return@withContext handleFiatRatesResult(throttledResult.toMap())

            runCatching { tangemTechApi.getRates(currencyId.lowercase(), coinIds.joinToString(",")) }
                .onSuccess { response ->
                    val ratesResultList: Map<String, Result<BigDecimal>> = response.rates.mapValues {
                        Result.Success(it.value.toBigDecimal())
                    }
                    val updatedCurrencies = throttledResult.toMap().toMutableMap()
                    coinsList.forEach { currency ->
                        ratesResultList[currency.coinId]?.let {
                            updatedCurrencies[currency] = it
                            throttler.updateThrottlingTo(currency)
                            throttler.setValue(currency, it)
                        }
                    }
                    return@withContext handleFiatRatesResult(updatedCurrencies)
                }
                .onFailure { Result.Failure(it) }

            error("Unreachable code because runCatching must return result")
        }

    private fun handleFiatRatesResult(rates: Map<Currency, Result<BigDecimal>?>): Result.Success<RatesResult> {
        val success = mutableMapOf<Currency, BigDecimal>()
        val failures = mutableMapOf<Currency, Throwable>()

        rates.mapNotNull { (currency, priceResult) ->
            when (priceResult) {
                is Result.Success -> success[currency] = priceResult.data
                is Result.Failure -> failures[currency] = priceResult.error
                else -> null
            }
        }

        return Result.Success(success to failures)
    }

    fun clear() {
        throttler.clear()
    }
}

typealias RatesResult = Pair<MutableMap<Currency, BigDecimal>, MutableMap<Currency, Throwable>>

val RatesResult.loadedRates
    get() = this.first

val RatesResult.failedRates
    get() = this.second
