package com.tangem.data.tokens.repository

import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.local.token.ExpressAssetsStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class DefaultMarketCryptoCurrencyRepository(
    private val expressAssetsStore: ExpressAssetsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MarketCryptoCurrencyRepository {

    override suspend fun isExchangeable(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        return getExchangeableFlag(userWalletId, cryptoCurrency) && !cryptoCurrency.isCustom
    }

    private suspend fun getExchangeableFlag(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        return withContext(dispatchers.io) {
            val contractAddress =
                (cryptoCurrency as? CryptoCurrency.Token)?.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE

            val asset = expressAssetsStore.getSyncOrNull(userWalletId)?.find {
                it.network == cryptoCurrency.network.backendId &&
                    it.contractAddress.equals(contractAddress, ignoreCase = true)
            }

            asset?.exchangeAvailable ?: false
        }
    }
}