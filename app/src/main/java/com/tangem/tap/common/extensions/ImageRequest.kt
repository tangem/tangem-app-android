package com.tangem.tap.common.extensions

import android.widget.ImageView
import coil.ImageLoader
import coil.imageLoader
import coil.load
import coil.request.Disposable
import coil.request.ImageRequest
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.wallet.R

/**
* [REDACTED_AUTHOR]
 */
fun ImageRequest.Builder.cardImageData(any: Any?): ImageRequest.Builder = apply {
    data(any?.parseData())
}

fun ImageView.loadCardImage(
    data: Any?,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {},
): Disposable = this.load(
    data = data?.parseData(),
    imageLoader = imageLoader,
    builder = builder,
)

private fun Any?.parseData(): Any? = when {
    this is String && this == Artwork.SALT_PAY_URL -> R.drawable.img_salt_pay_visa
    else -> this
}
