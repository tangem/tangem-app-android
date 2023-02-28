package com.tangem.tap.common.images

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.util.Logger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

private const val COIL_LOG_TAG = "COIL"

fun createCoilImageLoader(
    context: Context,
    logEnabled: Boolean = false,
): ImageLoader {
    return ImageLoader.Builder(context)
        .apply {
            if (!logEnabled) return@apply

            logger(CoilTimberLogger())
            okHttpClient {
                OkHttpClient.Builder()
                    .addNetworkInterceptor(
                        HttpLoggingInterceptor { message ->
                            Timber.tag(COIL_LOG_TAG).d(message)
                        }
                            .apply {
                                level = HttpLoggingInterceptor.Level.BODY
                            }
                    )
                    .build()
            }
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
