package com.tangem.blockchainsdk.providers

import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.config.providers.BlockchainProvidersStorage
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loader of [BlockchainProvidersResponse]
 *
 * @property tangemTechApi              tangem tech api
 * @property blockchainProvidersStorage blockchain providers storage
 * @property dispatchers                dispatchers
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class BlockchainProvidersResponseLoader @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val blockchainProvidersStorage: BlockchainProvidersStorage,
    private val blockchainProvidersResponseMerger: BlockchainProvidersResponseMerger,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /** Load [BlockchainProvidersResponse] */
    suspend fun load(): BlockchainProvidersResponse? {
        val localResponse = loadLocal().ifEmpty { return null }

        return loadRemote().fold(
            onSuccess = { remoteResponse ->
                blockchainProvidersResponseMerger.merge(
                    local = localResponse,
                    remote = remoteResponse,
                )
            },
            onFailure = {
                Timber.e(it, "Failed to load blockchain provider types from backend")
                localResponse
            },
        )
    }

    private suspend fun loadLocal(): BlockchainProvidersResponse = blockchainProvidersStorage.getConfigSync()

    private suspend fun loadRemote() = runCatching(
        dispatcher = dispatchers.io,
        block = tangemTechApi::getBlockchainProviders,
    )
}