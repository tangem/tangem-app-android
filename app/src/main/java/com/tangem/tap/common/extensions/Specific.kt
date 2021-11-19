package com.tangem.tap.common.extensions

import com.tangem.common.extensions.isZero
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.network.coinmarketcap.FiatCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

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

fun BigDecimal.toFormattedCurrencyString(
    decimals: Int, currency: String, roundingMode: RoundingMode = RoundingMode.DOWN,
    limitNumberOfDecimals: Boolean = true
): String {
    val decimalsForRounding = if (limitNumberOfDecimals){
        if (decimals > 8) 8 else decimals
    } else {
        decimals
    }
    val formattedAmount = this.toFormattedString(
        decimals = decimalsForRounding, roundingMode = roundingMode
    )
    return "$formattedAmount $currency"
}

fun BigDecimal.toFiatString(rateValue: BigDecimal, fiatCurrencyName: FiatCurrencyName): String {
    var fiatValue = rateValue.multiply(this)
    fiatValue = fiatValue.setScale(2, RoundingMode.HALF_UP)
    return "≈ ${fiatCurrencyName}  $fiatValue"
}

fun BigDecimal.toFiatValue(rateValue: BigDecimal): BigDecimal {
    val fiatValue = rateValue.multiply(this)
    return fiatValue.setScale(2, RoundingMode.HALF_UP)
}

fun BigDecimal.toFormattedFiatValue(fiatCurrencyName: FiatCurrencyName): String {
    return "≈ ${fiatCurrencyName}  $this"
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