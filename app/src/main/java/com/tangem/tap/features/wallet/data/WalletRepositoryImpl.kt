package com.tangem.tap.features.wallet.data

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.tap.features.wallet.domain.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of repository for Wallet feature
 *
 * @property tangemTechApi API for server requests
 */
class WalletRepositoryImpl(private val tangemTechApi: TangemTechApi) : WalletRepository {

    override suspend fun getCurrencyList(): CurrenciesResponse =
        withContext(Dispatchers.IO) { // TODO("After adding DI") replace with CoroutineDispatcherProvider
            tangemTechApi.getCurrencyList()
        }
}