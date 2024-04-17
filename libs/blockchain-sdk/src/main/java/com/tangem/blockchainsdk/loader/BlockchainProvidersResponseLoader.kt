package com.tangem.blockchainsdk.loader

import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.tangemTech.TangemTechServiceApi
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loader of [BlockchainProvidersResponse]
 *
 * @property tangemTechServiceApi tangem tech api
 * @property authProvider         auth provider
 * @property assetLoader          asset loader for local config loading
 * @property dispatchers          dispatchers
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class BlockchainProvidersResponseLoader @Inject constructor(
    private val tangemTechServiceApi: TangemTechServiceApi,
    private val authProvider: AuthProvider,
    private val assetLoader: AssetLoader,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /** Load [BlockchainProvidersResponse] */
    suspend fun load(): BlockchainProvidersResponse? {
        return runCatching(dispatcher = dispatchers.io, block = ::loadRemote)
            .fold(
                onSuccess = { it },
                onFailure = {
                    Timber.e(it, "Failed to load blockchain provider types from backend")
                    loadLocal()
                },
            )
    }

    private suspend fun loadRemote(): BlockchainProvidersResponse {
        return tangemTechServiceApi.getBlockchainProviders(
            cardPublicKey = authProvider.getCardPublicKey(),
            cardId = authProvider.getCardId(),
        )
    }

    private suspend fun loadLocal(): BlockchainProvidersResponse? {
        return assetLoader.load<BlockchainProvidersResponse>(fileName = PROVIDER_TYPES_FILE_NAME)
    }

    private companion object {
        const val PROVIDER_TYPES_FILE_NAME = "tangem-app-config/providers_order"
    }
}