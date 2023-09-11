package com.tangem.data.tokens.repository

import com.tangem.datasource.local.token.UserMarketCoinsStore
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.wallets.models.UserWalletId

class DefaultMarketCryptoCurrencyRepository(
    private val userMarketCoinsStore: UserMarketCoinsStore,
) : MarketCryptoCurrencyRepository {

    override suspend fun isExchangeable(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Boolean {
        return userMarketCoinsStore.getSyncOrNull(userWalletId)?.coins
            ?.firstOrNull { it.id == cryptoCurrencyId.rawCurrencyId }
            ?.networks
            ?.firstOrNull { it.networkId == cryptoCurrencyId.rawNetworkId }?.exchangeable ?: false
    }
}