package com.tangem.blockchainsdk.loader

import androidx.core.util.PatternsCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.blockchainsdk.utils.createPrivateProviderType
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.config.models.ProviderModel
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loader of [BlockchainProvidersResponse]
 *
 * @property tangemTechServiceApi tangem tech api
 * @property assetLoader          asset loader for local config loading
 * @property dispatchers          dispatchers
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class BlockchainProvidersResponseLoader @Inject constructor(
    private val tangemTechServiceApi: TangemTechApi,
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

    private suspend fun loadRemote() = tangemTechServiceApi.getBlockchainProviders()

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
        val remoteWithoutInvalidProviders = remote
            .mapValues {
                it.value
                    .filterUnsupportedProviders()
                    .filterInvalidProviders()
            }
            .filterValues { it.isNotEmpty() }

        val result = local + remoteWithoutInvalidProviders

        if (result != remote) {
            val missingBlockchains = result.keys - remote.keys
            val blockchainsWithoutProviders = remote.filterValues { it.isEmpty() }.keys

            recordException(missingBlockchains = missingBlockchains + blockchainsWithoutProviders)
        }

        return result.guaranteeUrlsEndWithSlash()
    }

    private fun List<ProviderModel>.filterUnsupportedProviders() = filter {
        val isSupportedType = it !is ProviderModel.UnsupportedType

        val isSupportedPrivateType = if (it is ProviderModel.Private) {
            createPrivateProviderType(it.name) != null
        } else {
            true
        }

        isSupportedType && isSupportedPrivateType
    }

    private fun List<ProviderModel>.filterInvalidProviders() = mapNotNull { provider ->
        if (provider is ProviderModel.Public) {
            if (isValidUrl(provider.url)) provider else null
        } else {
            provider
        }
    }

    private fun isValidUrl(url: String): Boolean {
        val forbiddenScheme = forbiddenSchemes.firstOrNull { url.startsWith(prefix = it) }
        val inputUrl = if (forbiddenScheme != null) url.substringAfter(forbiddenScheme) else url

        return PatternsCompat.WEB_URL.matcher(inputUrl).matches()
    }

    private fun recordException(missingBlockchains: Set<String>) {
        val exception = IllegalStateException(
            "Remote config does not contain required blockchains or providers information: " +
                missingBlockchains.joinToString(),
        )

        Timber.e(exception)

        firebaseCrashlytics.recordException(exception)
    }

    /*
     * Example:
     * https://qwe.com --> https://qwe.com/
     */
    private fun BlockchainProvidersResponse.guaranteeUrlsEndWithSlash(): BlockchainProvidersResponse {
        return mapValues {
            it.value.map { provider -> provider.addSlashIfAbsent() }
        }
    }

    private fun ProviderModel.addSlashIfAbsent(): ProviderModel {
        return if (this is ProviderModel.Public && url.last() != '/') {
            copy(url = "$url/")
        } else {
            this
        }
    }

    private companion object {
        const val PROVIDER_TYPES_FILE_NAME = "tangem-app-config/providers_order"

        val forbiddenSchemes = listOf("wss://")
    }
}