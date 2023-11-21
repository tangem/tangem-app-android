package com.tangem.tap.common.extensions

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

@Suppress("MagicNumber")
fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 20, stream)
    return stream.toByteArray()
}