package com.tangem.tap.network.coinmarketcap

import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class CoinMarketCapService() {
    private val api: CoinMarketCapApi by lazy { CoinMarketCapApi.create(getApiKey()) }

    private fun getApiKey(): String {
        return store.state.globalState.configManager?.config?.coinMarketCapKey ?: ""
    }

    suspend fun getRate(
            currency: String, fiatCurrency: FiatCurrencyName? = null
    ): Result<BigDecimal> = withContext(Dispatchers.IO) {
        val response = performRequest { api.getRateInfo(1, currency, fiatCurrency) }
        return@withContext when (response) {
            is Result.Success -> Result.Success(response.data.data.getRate())
            is Result.Failure -> response
        }
    }

    suspend fun getFiatCurrencies(): Result<List<FiatCurrency>> = withContext(Dispatchers.IO) {
        val response = performRequest { api.getFiatMap() }
        return@withContext when (response) {
            is Result.Success -> Result.Success(response.data.data.sortedBy { it.name })
            is Result.Failure -> response
        }
    }
}