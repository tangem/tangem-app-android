package com.tangem.common.utils

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/** Decodes the text encoded in a QR-code [bitmap] (e.g. captured from a Compose node via captureToImage). */
fun decodeQrCode(bitmap: Bitmap): String {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val source = RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
    val hints = mapOf(DecodeHintType.TRY_HARDER to true)

    return QRCodeReader().decode(binaryBitmap, hints).text
}