package com.tangem.tap.domain

import com.tangem.common.services.Result
import com.tangem.domain.common.ThrottlerWithValues
import com.tangem.network.api.tangemTech.TangemTechService
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.store
import java.math.BigDecimal

//TODO: refactoring: move to domain
class RatesRepository {

    private val tangemTechService: TangemTechService
        get() = store.state.domainNetworks.tangemTechService

    private val throttler = ThrottlerWithValues<Currency, Result<BigDecimal>?>(60000)

    suspend fun loadFiatRate(currencyId: String, coinsList: List<Currency>): Result<RatesResult> {
//         get and submit previous result of equivalents.
        val throttledResult = coinsList.filter { throttler.isStillThrottled(it) }.map {
            Pair(it, throttler.geValue(it))
        }
        if (throttledResult.isNotEmpty()) {
            return handleFiatRatesResult(throttledResult.toMap())
        }

        val currenciesToUpdate = coinsList.filter { !throttler.isStillThrottled(it) }
        val coinIds = currenciesToUpdate.mapNotNull { it.coinId }.distinct()
        if (coinIds.isEmpty()) return EMPTY_RESULT

        return when (val result = tangemTechService.rates(currencyId, coinIds)) {
            is Result.Success -> {
                val ratesResultList: Map<String, Result<BigDecimal>> = result.data.rates.mapValues {
                    Result.Success(it.value.toBigDecimal())
                }
                val updatedCurrencies = mutableMapOf<Currency, Result<BigDecimal>?>()
                currenciesToUpdate.forEach { currency ->
                    ratesResultList[currency.coinId]?.let {
                        updatedCurrencies[currency] = it
                        throttler.updateThrottlingTo(currency)
                        throttler.setValue(currency, it)
                    }
                }
                handleFiatRatesResult(updatedCurrencies)
            }
            is Result.Failure -> Result.Failure(result.error)
        }
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

    companion object {
        val EMPTY_RESULT = Result.Success(Pair(
            mutableMapOf<Currency, BigDecimal>(),
            mutableMapOf<Currency, Throwable>()
        ))
    }
}

typealias RatesResult = Pair<MutableMap<Currency, BigDecimal>, MutableMap<Currency, Throwable>>

val RatesResult.loadedRates
    get() = this.first

val RatesResult.failedRates
    get() = this.second