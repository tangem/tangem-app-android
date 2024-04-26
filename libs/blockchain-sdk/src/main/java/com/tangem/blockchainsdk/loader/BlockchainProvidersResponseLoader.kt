package com.tangem.blockchainsdk.loader

import com.google.firebase.crashlytics.FirebaseCrashlytics
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

    private val firebaseCrashlytics by lazy(FirebaseCrashlytics::getInstance)

    /** Load [BlockchainProvidersResponse] */
    suspend fun load(): BlockchainProvidersResponse? {
        val localResponse = loadLocal() ?: return null

        return runCatching(dispatcher = dispatchers.io, block = ::loadRemote)
            .fold(
                onSuccess = { remoteResponse -> mergeResponses(local = localResponse, remote = remoteResponse) },
                onFailure = {
                    Timber.e(it, "Failed to load blockchain provider types from backend")
                    localResponse
                },
            )
    }

    private suspend fun loadLocal(): BlockchainProvidersResponse? {
        return assetLoader.load<BlockchainProvidersResponse>(fileName = PROVIDER_TYPES_FILE_NAME)
    }

    private suspend fun loadRemote(): BlockchainProvidersResponse {
        return tangemTechServiceApi.getBlockchainProviders(
            cardPublicKey = authProvider.getCardPublicKey(),
            cardId = authProvider.getCardId(),
        )
    }

    /** Merge blockchains with non-empty providers [remote] from remote with blockchains from local [local] */
    private fun mergeResponses(
        local: BlockchainProvidersResponse,
        remote: BlockchainProvidersResponse,
    ): BlockchainProvidersResponse {
        /*
         * Example:
         * val remote = mapOf("a" to 1, "b" to 2, "c" to 3)
         * val local = mapOf("a" to 11, "e" to 4, "f" to 5)
         *
         * local + remote // { a = 1, e = 4, f = 5, b = 2, c = 3 }
         */
        val result = local + remote.filterValues { it.isNotEmpty() }

        if (result != remote) {
            val missingBlockchains = result.keys - remote.keys
            val blockchainsWithoutProviders = remote.filterValues { it.isEmpty() }.keys

            recordException(missingBlockchains = missingBlockchains + blockchainsWithoutProviders)
        }

        return result
    }

    private fun recordException(missingBlockchains: Set<String>) {
        val exception = IllegalStateException(
            "Remote config does not contain required blockchains or providers information: " +
                missingBlockchains.joinToString(),
        )

        Timber.e(exception)

        firebaseCrashlytics.recordException(exception)
    }

    private companion object {
        const val PROVIDER_TYPES_FILE_NAME = "tangem-app-config/providers_order"
    }
}