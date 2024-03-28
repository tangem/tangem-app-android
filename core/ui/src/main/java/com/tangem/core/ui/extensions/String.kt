package com.tangem.core.ui.extensions

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Hashtable

@Suppress("MagicNumber")
fun String.toQrCode(sizePx: Int = 256, paddingPx: Int = 0): Bitmap {
    val hintMap = Hashtable<EncodeHintType, Any>()
    hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M // H = 30% damage
    hintMap[EncodeHintType.MARGIN] = paddingPx

    val qrCodeWriter = QRCodeWriter()

    val bitMatrix = qrCodeWriter.encode(this, BarcodeFormat.QR_CODE, sizePx, sizePx, hintMap)
    val width = bitMatrix.width
    val bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until width) {
            bmp.setPixel(
                y,
                x,
                if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE,
            )
        }
    }
    return bmp
}

fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
