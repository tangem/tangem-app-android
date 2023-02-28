package com.tangem.tap.common.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.*

fun String?.ellipsizeBeforeSpace(allowedSize: Int): String {
    if (this.isNullOrBlank()) return ""
    val size = this.length
    val sizeDifference = size - allowedSize
    val endIndex = this.indexOf(" ")
    val startIndex = endIndex - sizeDifference
    val newString = this.removeRange(startIndex, endIndex)
    return newString.substring(0 until startIndex) + "..." +
        newString.substring(startIndex until newString.length)
}

fun String.colorSegment(
    context: Context,
    color: Int,
    startIndex: Int = 0,
    endIndex: Int = this.length,
): Spannable {
    return this.toSpannable()
        .also { spannable ->
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, color)),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
}

@Suppress("MagicNumber")
fun String.toQrCode(): Bitmap {
    val hintMap = Hashtable<EncodeHintType, Any>()
    hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M // H = 30% damage
    hintMap[EncodeHintType.MARGIN] = 2

    val qrCodeWriter = QRCodeWriter()

    val size = 256

    val bitMatrix = qrCodeWriter.encode(this, BarcodeFormat.QR_CODE, size, size, hintMap)
    val width = bitMatrix.width
    val bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until width) {
            bmp.setPixel(y, x, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
    }
    return bmp
}

fun String.urlEncode(): String = Uri.encode(this)

fun String.removePrefixOrNull(prefix: String): String? = when {
    startsWith(prefix) -> substring(prefix.length)
    else -> null
}
