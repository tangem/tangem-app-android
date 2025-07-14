package com.tangem.tap.common.images

import android.content.Context
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.Logger
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import okhttp3.OkHttpClient
import timber.log.Timber

private const val COIL_LOG_TAG = "COIL"
private const val COIL_MEMORY_CACHE_SIZE = 0.25

fun createCoilImageLoader(context: Context, logEnabled: Boolean = false): ImageLoader {
    return ImageLoader.Builder(context)
        .apply {
            if (!logEnabled) return@apply

            logger(CoilTimberLogger())
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

private class CoilTimberLogger : Logger {

    override var level: Int = Log.DEBUG

    override fun log(tag: String, priority: Int, message: String?, throwable: Throwable?) {
        with(Timber.tag(COIL_LOG_TAG)) {
            throwable?.let { e -> e(e, message) }
            message?.let { msg -> d(msg) }
        }
    }
}