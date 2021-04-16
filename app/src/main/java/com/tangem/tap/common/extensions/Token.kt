package com.tangem.tap.common.extensions

import androidx.annotation.ColorInt
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