package com.tangem.tap.common.extensions

import coil.request.ImageRequest

/**
 * Created by Anton Zhilenkov on 18.02.2023.
 */
fun ImageRequest.Builder.cardImageData(any: Any?): ImageRequest.Builder = apply {
    data(any)
}
