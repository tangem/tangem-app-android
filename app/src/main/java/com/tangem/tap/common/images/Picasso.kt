package com.tangem.tap.common.images

import android.app.Application
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.tangem.wallet.BuildConfig
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class PicassoHelper {
    companion object {
        private const val KEEP_CACHE_MAX_DAYS = 7

        fun initPicassoWithCaching(application: Application) {
            val picasso = Picasso.Builder(application)
                    .downloader(OkHttp3Downloader(getOkHttpForPicasso(application)))
                    .build()
            picasso.isLoggingEnabled = BuildConfig.DEBUG
            picasso.setIndicatorsEnabled(BuildConfig.DEBUG)
            Picasso.setSingletonInstance(picasso)
        }

        private fun getOkHttpForPicasso(application: Application): OkHttpClient {
            val okHttpBuilder = OkHttpClient.Builder()
            okHttpBuilder.cache(Cache(File(application.filesDir, "artworks"), Long.MAX_VALUE))
            okHttpBuilder.callTimeout(15000, TimeUnit.MILLISECONDS)

            okHttpBuilder.addInterceptor { chain ->
                val cacheControl = CacheControl.Builder()
                        .maxStale(KEEP_CACHE_MAX_DAYS, TimeUnit.DAYS)
                        .build()
                val origRequest = chain.request()
                val neverExpireRequest = origRequest.newBuilder()
                        .cacheControl(cacheControl)
                        .build()
                chain.proceed(neverExpireRequest)
            }
            addDebugInterceptors(okHttpBuilder)

            return okHttpBuilder.build()
        }

        private fun addDebugInterceptors(okHttpBuilder: OkHttpClient.Builder) {
            if (!BuildConfig.DEBUG) return

            val picassoInterceptor = HttpLoggingInterceptor(PicassoOkHttpLogger()).apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpBuilder.addInterceptor(picassoInterceptor)
        }

    }
}

private class PicassoOkHttpLogger : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        Timber.d(message)
    }
}