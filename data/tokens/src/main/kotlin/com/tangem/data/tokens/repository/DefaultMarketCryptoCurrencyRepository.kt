package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.local.token.AssetsStore
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.wallets.models.UserWalletId

class DefaultMarketCryptoCurrencyRepository(
    private val assetsStore: AssetsStore,
) : MarketCryptoCurrencyRepository {

    override suspend fun isExchangeable(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        val cryptoCurrencyId = cryptoCurrency.id
        val blockchain = Blockchain.fromId(cryptoCurrencyId.rawNetworkId)
        val apiNetworkId = blockchain.toNetworkId()
        val contractAddress = (cryptoCurrency as? CryptoCurrency.Token)?.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE

        return assetsStore.getSyncOrNull(userWalletId)?.find {
            it.network == apiNetworkId &&
                it.token == cryptoCurrencyId.rawCurrencyId &&
                it.contractAddress == contractAddress &&
                it.isActive
        }?.exchangeAvailable ?: false
    }
}