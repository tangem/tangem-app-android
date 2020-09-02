package com.tangem.tap.network.coinmarketcap

import com.tangem.commands.common.network.Result
import com.tangem.commands.common.network.performRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class CoinMarketCapService {
    private val api: CoinMarketCapApi by lazy { CoinMarketCapApi.create() }

    suspend fun getRate(currency: String): Result<BigDecimal> = withContext(Dispatchers.IO) {
        val response = performRequest { api.getRateInfo(1, currency) }
        return@withContext when (response) {
            is Result.Success -> Result.Success(response.data.data.quote.usd.price)
            is Result.Failure -> response
        }
    }
}