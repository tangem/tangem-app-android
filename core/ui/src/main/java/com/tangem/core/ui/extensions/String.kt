package com.tangem.core.ui.extensions

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.tangem.core.res.getPluralStringSafe
import com.tangem.core.res.getStringSafe
import com.tangem.utils.StringsSigns.THREE_STARS
import java.util.Hashtable

/**
 * Get a string resource safely or the resource name if an exception is thrown
 *
 * @param id         resource id
 * @param formatArgs format args
 */
@Composable
@ReadOnlyComposable
fun stringResourceSafe(@StringRes id: Int, vararg formatArgs: Any): String {
    val resources = LocalContext.current.resources

    return if (formatArgs.isEmpty()) {
        resources.getStringSafe(id)
    } else {
        resources.getStringSafe(id, *formatArgs)
    }
}

/**
 * Get a plural string resource safely or the resource name if an exception is thrown
 *
 * @param id         resource id
 * @param count      count
 * @param formatArgs format args
 */
@Composable
@ReadOnlyComposable
fun pluralStringResourceSafe(@PluralsRes id: Int, count: Int, vararg formatArgs: Any): String {
    val resources = LocalContext.current.resources

    return if (formatArgs.isEmpty()) {
        resources.getPluralStringSafe(id, count)
    } else {
        resources.getPluralStringSafe(id, count, *formatArgs)
    }
}

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

fun String.orMaskWithStars(maskWithStars: Boolean): String {
    return if (maskWithStars) THREE_STARS else this
}

/**
 * Returns a masked representation of the string for safe display.
 *
 * Rules:
 * - empty string: returned unchanged
 * - length <= 2: all characters are replaced with '*'
 * - length <= 4: keep the first and last characters, mask the middle
 * - length > 4: keep the first two and last two characters, mask the middle with "**"
 *
 * @return masked string according to the rules above
 */
@Suppress("MagicNumber")
fun String.mask(): String {
    return when {
        this.isEmpty() -> this
        this.length <= 2 -> "*".repeat(this.length) // mask all if too short
        this.length <= 4 -> this.first() + "*".repeat(this.length - 2) + this.last()
        else -> {
            val prefix = this.take(2)
            val suffix = this.takeLast(2)
            "$prefix**$suffix"
        }
    }
}