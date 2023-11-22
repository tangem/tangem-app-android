package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.local.token.UserMarketCoinsStore
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.wallets.models.UserWalletId

class DefaultMarketCryptoCurrencyRepository(
    private val userMarketCoinsStore: UserMarketCoinsStore,
) : MarketCryptoCurrencyRepository {

    override suspend fun isExchangeable(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Boolean {
        val blockchain = Blockchain.fromId(cryptoCurrencyId.rawNetworkId)
        val apiNetworkId = blockchain.toNetworkId()
        return userMarketCoinsStore.getSyncOrNull(userWalletId)?.coins
            ?.firstOrNull { it.id == cryptoCurrencyId.rawCurrencyId }
            ?.networks
            ?.firstOrNull {
                if (it.contractAddress != null) {
                    it.networkId == apiNetworkId && it.contractAddress == cryptoCurrencyId.contractAddress
                } else {
                    it.networkId == apiNetworkId
                }
            }?.exchangeable ?: false
    }
}