package com.tangem.tap.network.coinmarketcap

import com.tangem.commands.common.network.Result
import com.tangem.commands.common.network.performRequest
import java.math.BigDecimal

class CoinMarketCapService {
    private val api: CoinMarketCapApi by lazy { CoinMarketCapApi.create() }

    suspend fun getRate(currency: String): Result<BigDecimal> {
        val response = performRequest { api.getRateInfo(1, currency) }
        return when (response) {
            is Result.Success -> Result.Success(response.data.data.quote.usd.price)
            is Result.Failure -> response
        }
    }
}