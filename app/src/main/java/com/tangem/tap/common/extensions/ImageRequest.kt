package com.tangem.tap.common.extensions

import coil.request.ImageRequest

/**
[REDACTED_AUTHOR]
 */
fun ImageRequest.Builder.cardImageData(any: Any?): ImageRequest.Builder = apply {
    data(any)
}