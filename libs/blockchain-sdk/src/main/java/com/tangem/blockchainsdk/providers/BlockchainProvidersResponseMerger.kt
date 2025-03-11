package com.tangem.blockchainsdk.providers

import androidx.core.util.PatternsCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.datasource.local.config.providers.models.ProviderModel
import timber.log.Timber

/**
 * Merger of [BlockchainProvidersResponse]
 *
[REDACTED_AUTHOR]
 */
internal object BlockchainProvidersResponseMerger {

    private val firebaseCrashlytics by lazy(FirebaseCrashlytics::getInstance)

    private val forbiddenSchemes = listOf("wss://")

    /**
     * Merge blockchains with non-empty providers from [remote] with blockchains from [local]
     *
     * Example:
     * val remote = mapOf("a" to 1, "b" to 2, "c" to 3)
     * val local = mapOf("a" to 11, "e" to 4, "f" to 5)
     *
     * local + remote // { a = 1, e = 4, f = 5, b = 2, c = 3 }
     */
    fun merge(local: BlockchainProvidersResponse, remote: BlockchainProvidersResponse): BlockchainProvidersResponse {
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

    private fun List<ProviderModel>.filterUnsupportedProviders() = filter { model ->
        val isSupportedType = model !is ProviderModel.UnsupportedType

        val isSupportedPrivateType = if (model is ProviderModel.Private) {
            ProviderTypeIdMapping.entries.any { it.id == model.name }
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
}