package com.tangem.tap.common.extensions

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.luminance
import androidx.core.graphics.toColorInt
import com.tangem.blockchain.common.Token
import com.tangem.wallet.R

@ColorInt
fun Token.getColor(): Int {
    return try {
        ("#" + this.contractAddress.subSequence(2..7).toString())
            .toColorInt()
    } catch (exception: Exception) {
        R.color.lightGray4
    }
}

@ColorInt
fun Token.getTextColor(): Int {
    return if (this.getColor().luminance > 0.5) Color.BLACK else Color.WHITE
}