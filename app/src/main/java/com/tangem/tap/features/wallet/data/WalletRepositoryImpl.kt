package com.tangem.tap.features.wallet.data

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.tap.features.wallet.domain.WalletRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Implementation of repository for Wallet feature
 *
 * @property tangemTechApi API for server requests
 * @property dispatchers   coroutine dispatcher provider
 */
class WalletRepositoryImpl(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletRepository {

    override suspend fun getCurrencyList(): CurrenciesResponse = withContext(dispatchers.io) {
        tangemTechApi.getCurrencyList()
    }
}
