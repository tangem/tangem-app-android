package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.local.token.AssetsStore
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.wallets.models.UserWalletId
import timber.log.Timber

class DefaultMarketCryptoCurrencyRepository(
    private val assetsStore: AssetsStore,
) : MarketCryptoCurrencyRepository {

    override suspend fun isExchangeable(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Boolean {
        val blockchain = Blockchain.fromId(cryptoCurrencyId.rawNetworkId)
        val apiNetworkId = blockchain.toNetworkId()

        return assetsStore.getSyncOrNull(userWalletId)?.find {
            it.network == apiNetworkId
                && it.token == cryptoCurrencyId.rawCurrencyId
                && it.isActive
        }?.exchangeAvailable ?: false

    }
}
