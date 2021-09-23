package com.tangem.tap.domain

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.tangem.common.services.Result

/**
[REDACTED_AUTHOR]
 */
class UrlBitmapLoader {

    fun loadBitmap(url: String, callback: (Result<Bitmap>) -> Unit) {
        Picasso.get().load(url).into(DownloadTarget(callback))
    }

    fun loadBitmap(url: String, target: DownloadTarget) {
        Picasso.get().load(url).into(target)
    }
}

// It adds the ability to trigger multiple downloads with a unique callback.
open class DownloadTarget(
    val callback: (Result<Bitmap>) -> Unit,
) : Target {

    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
        callback(Result.Success(bitmap))
    }

    override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: Drawable?) {
        callback.invoke(Result.Failure(e))
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
    }
}