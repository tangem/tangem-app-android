package com.tangem.tap.features.home.data

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.GeoResponse
import com.tangem.tap.features.home.domain.HomeRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Implementation of repository for Home feature
 *
 * @property tangemTechApi API for server requests
 * @property dispatchers   coroutine dispatcher provider
 */
class HomeRepositoryImpl(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : HomeRepository {

    override suspend fun getUserCountryCode(): GeoResponse = withContext(dispatchers.io) {
        tangemTechApi.getUserCountryCode()
    }
}
