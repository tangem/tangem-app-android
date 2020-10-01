package com.tangem.tap.common.images

import android.app.Application
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class PicassoHelper {
    companion object {
        private const val KEEP_CACHE_MAX_DAYS = 7

        fun initPicassoWithCaching(application: Application) {
            val picasso = Picasso.Builder(application)
                    .downloader(OkHttp3Downloader(getOkHttpForPicasso(application)))
                    .build()
            Picasso.setSingletonInstance(picasso)
        }

        private fun getOkHttpForPicasso(application: Application): OkHttpClient {
            val okHttpBuilder = OkHttpClient.Builder()
            okHttpBuilder.cache(Cache(File(application.filesDir, "artworks"), Long.MAX_VALUE))
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
            return okHttpBuilder.build()
        }
    }
}