package com.tangem.tap.common.images

import android.content.Context
import android.os.Build
import android.util.Log
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
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.utils.logging.TangemLogger
import okhttp3.OkHttpClient
import coil.util.Logger as CoilLogger

private const val COIL_LOG_TAG = "COIL"
private const val COIL_MEMORY_CACHE_SIZE = 0.25

fun createCoilImageLoader(context: Context, logEnabled: Boolean = false): ImageLoader {
    return ImageLoader.Builder(context)
        .eventListener(CoilErrorEventListener())
        .apply {
            if (!logEnabled) return@apply

            logger(CoilKermitLogger())
            okHttpClient {
                OkHttpClient.Builder()
                    .addNetworkInterceptor(createNetworkLoggingInterceptor())
                    .build()
            }
        }
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
 *
 * Registered unconditionally, unlike [CoilKermitLogger] and the logging OkHttp client above: those
 * dump every request — including image bytes at `Level.BODY` — and stay behind `logEnabled`.
 */
private class CoilErrorEventListener : EventListener {

    private val logger = TangemLogger.withTag(COIL_LOG_TAG)

    override fun onError(request: ImageRequest, result: ErrorResult) {
        // A null url is an absent image, not a failed download — every asset without media would
        // otherwise report one, drowning the real failures.
        if (result.throwable is NullRequestDataException) return

        logger.e("Failed to load image: ${request.data}", result.throwable)
    }
}

private class CoilKermitLogger : CoilLogger {

    override var level: Int = Log.DEBUG
    private val logger = TangemLogger.withTag(COIL_LOG_TAG)

    override fun log(tag: String, priority: Int, message: String?, throwable: Throwable?) {
        if (throwable != null) {
            logger.e(message ?: "<EMPTY>", throwable)
        } else if (message != null) {
            logger.d(message)
        }
    }
}