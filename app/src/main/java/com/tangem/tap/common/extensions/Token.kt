package com.tangem.tap.common.extensions

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.luminance
import androidx.core.graphics.toColorInt
import com.tangem.blockchain.common.Token

@Suppress("MagicNumber")
@ColorInt
fun Token.getColor(isTestnet: Boolean = false): Int {
    val defaultColor = "#C7C7CC".toColorInt() // equivalent to R.color.lightGray4

    return if (isTestnet) {
        defaultColor
    } else {
        try {
            ("#" + this.contractAddress.subSequence(2..7).toString()).toColorInt()
        } catch (exception: Exception) {
            defaultColor
        }
    }
}

@Suppress("MagicNumber")
@ColorInt
fun Token.getTextColor(isTestnet: Boolean = false): Int = when {
    isTestnet -> Color.WHITE
    this.getColor().luminance > 0.5 -> Color.BLACK
    else -> Color.WHITE
}
