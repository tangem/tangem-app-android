package com.tangem.blockchainsdk.providers

import android.os.Build
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loader of [BlockchainProvidersResponse]
 *
 * @property blockchainProvidersApi     blockchain providers api
 * @property blockchainProvidersStorage blockchain providers storage
 * @property dispatchers                dispatchers
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class BlockchainProvidersResponseLoader @Inject constructor(
    private val blockchainProvidersApi: BlockchainProvidersApi,
    private val blockchainProvidersStorage: BlockchainProvidersStorage,
    private val blockchainProvidersResponseMerger: BlockchainProvidersResponseMerger,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /** Load [BlockchainProvidersResponse] */
    suspend fun load(): BlockchainProvidersResponse? {
        val localResponse = loadLocal()

        /**
         * Behavior when local config is empty or cannot be parsed:
         * - Android 15          -> return null (do not load remote)
         * - Android 16 (stable) -> return null (do not load remote)
         * - Android 16 (preview)-> continue (attempt to load remote and merge)
         * - Android 17+         -> continue (attempt to load remote and merge)
         */
        val shouldLoadRemoteIfError = Build.VERSION.SDK_INT > Build.VERSION_CODES.BAKLAVA ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.BAKLAVA && Build.VERSION.PREVIEW_SDK_INT > 0

        if (localResponse.isEmpty() && !shouldLoadRemoteIfError) {
            return null
        }

        return loadRemote().fold(
            onSuccess = { remoteResponse ->
                blockchainProvidersResponseMerger.merge(
                    local = localResponse,
                    remote = remoteResponse,
                )
            },
            onFailure = { throwable ->
                TangemLogger.e("Failed to load blockchain provider types from backend", throwable)
                localResponse.ifEmpty { null }
            },
        )
    }

    private suspend fun loadLocal(): BlockchainProvidersResponse = blockchainProvidersStorage.getConfigSync()

    private suspend fun loadRemote() = runCatching(
        dispatcher = dispatchers.io,
        block = blockchainProvidersApi::getBlockchainProviders,
    )
}