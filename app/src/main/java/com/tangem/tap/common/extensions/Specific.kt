package com.tangem.tap.common.extensions

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.network.coinmarketcap.FiatCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


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

fun BigDecimal.toFormattedString(
        decimals: Int, roundingMode: RoundingMode = RoundingMode.DOWN, locale: Locale = Locale.US
): String {
    val symbols = DecimalFormatSymbols(locale)
    val df = DecimalFormat()
    df.decimalFormatSymbols = symbols
    df.maximumFractionDigits = decimals
    df.minimumFractionDigits = 0
    df.isGroupingUsed = false
    df.roundingMode = roundingMode
    return df.format(this)
}

fun BigDecimal.toFormattedCurrencyString(decimals: Int, currency: String): String {
    return "${this.toFormattedString(decimals)} $currency"
}

fun BigDecimal.toFiatString(rateValue: BigDecimal, fiatCurrencyName: FiatCurrencyName): String? {
    var fiatValue = rateValue.multiply(this)
    fiatValue = fiatValue.setScale(2, RoundingMode.DOWN)
    return "≈ ${fiatCurrencyName}  $fiatValue"
}

fun FiatCurrency.toFormattedString(): String = "${this.name} (${this.symbol}) - ${this.sign}"

fun BigDecimal.stripZeroPlainString(): String = this.stripTrailingZeros().toPlainString()

// 0.00 -> 0.00
// 0.00002345 -> 0.00002
// 1.00002345 -> 1.00
// 1.45002345 -> 1.45
fun BigDecimal.scaleToFiat(applyPrecision: Boolean = false): BigDecimal {
    if (this.isZero()) return this

    val scaledFiat = this.setScale(2, RoundingMode.DOWN)
    return if (scaledFiat.isZero() && applyPrecision) this.setPrecision(1)
    else scaledFiat

}

fun BigDecimal.setPrecision(precision: Int, roundingMode: RoundingMode = RoundingMode.DOWN): BigDecimal {
    if (precision == precision() || scale() <= precision) return this
    return this.setScale(scale() - precision() + precision, roundingMode)
}

fun BigDecimal.isPositive(): Boolean = this.compareTo(BigDecimal.ZERO) == 1
fun BigDecimal.isNegative(): Boolean = this.compareTo(BigDecimal.ZERO) == -1
fun BigDecimal.isGreaterThan(value: BigDecimal): Boolean = this.compareTo(value) == 1
fun BigDecimal.isLessThan(value: BigDecimal): Boolean = this.compareTo(value) == -1

fun BigDecimal.isGreaterThanOrEqual(value: BigDecimal): Boolean {
    val compareResult = this.compareTo(value)
    return compareResult == 1 || compareResult == 0
}

fun BigDecimal.isLessThanOrEqual(value: BigDecimal): Boolean {
    val compareResult = this.compareTo(value)
    return compareResult == -1 || compareResult == 0
}