package com.tangem.tap.common.images

import android.content.Context
import coil.imageLoader
import coil.request.Disposable
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.tangem.core.ui.coil.ImagePreloader

internal class DefaultImagePreloader(
    private val appContext: Context,
) : ImagePreloader {
    override fun preload(url: String): Disposable {
        return appContext.imageLoader.enqueue(
            ImageRequest.Builder(appContext)
                .data(url)
                .memoryCacheKey(url)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
        )
    }
}