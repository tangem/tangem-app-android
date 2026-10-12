package com.tangem.tap.common.images

import android.content.Context
import android.os.Build
import coil.EventListener
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestDataException
import com.tangem.utils.logging.TangemLogger
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val COIL_LOG_TAG = "COIL"
private const val COIL_MEMORY_CACHE_SIZE = 0.25

internal fun createCoilImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .eventListener(CoilErrorEventListener())
        .components {
            // According to Coil ImageDecoder API is faster and supports animated WebP and HEIF
            // https://coil-kt.github.io/coil/gifs/
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
            add(SvgDecoder.Factory())
        }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(COIL_MEMORY_CACHE_SIZE)
                .build()
        }
        .build()
}

/**
 * Reports every failed image request through [TangemLogger], so that a broken NFT preview or token
 * icon is diagnosable from the log file a user attaches to a support request.
 */
private class CoilErrorEventListener : EventListener {

    private val logger = TangemLogger.withTag(COIL_LOG_TAG)

    override fun onError(request: ImageRequest, result: ErrorResult) {
        // A null url is an absent image, not a failed download — every asset without media would
        // otherwise report one, drowning the real failures.
        if (result.throwable is NullRequestDataException) return

        logger.e("Failed to load image: ${request.data.redactQueryValues()}", result.throwable)
    }

    /**
     * Credential-bearing parts — query values, userinfo, fragment — are dropped because this line lands
     * in the log file a user attaches to a support request, and the log sanitizer masks hex-like
     * sequences only, so a signed url's token would survive it. Parameter *names* are kept: whether a
     * url arrived with its query intact is exactly what a broken-image report has to answer. Everything
     * that identifies the endpoint, the port included, is preserved.
     */
    private fun Any?.redactQueryValues(): String {
        val raw = toString()
        val redacted = raw.toHttpUrlOrNull()?.redactCredentials() ?: raw

        // A long path, a long parameter list or a data: uri would otherwise bloat the log file.
        return redacted.take(n = MAX_LOGGED_DATA_LENGTH)
    }

    private fun HttpUrl.redactCredentials(): String {
        val base = newBuilder()
            .username("")
            .password("")
            .query(null)
            .fragment(null)
            .build()
            .toString()

        return if (querySize == 0) {
            base
        } else {
            queryParameterNames.joinToString(prefix = "$base?", separator = "&") { "$it=***" }
        }
    }

    private companion object {
        const val MAX_LOGGED_DATA_LENGTH = 512
    }
}