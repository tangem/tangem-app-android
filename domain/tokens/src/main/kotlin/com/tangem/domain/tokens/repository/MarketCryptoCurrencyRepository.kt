package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

/**
 * MarketCryptoCurrencyRepository works with data from Tangem coins backend, CoinMarketCap etc
 */
interface MarketCryptoCurrencyRepository {

    suspend fun isExchangeable(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean
}
