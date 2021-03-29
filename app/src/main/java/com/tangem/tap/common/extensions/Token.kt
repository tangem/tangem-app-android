package com.tangem.tap.common.extensions

import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import com.tangem.blockchain.common.Token

@ColorInt
fun Token.getColor(): Int {
    return ("#" + this.contractAddress.subSequence(2..7).toString())
            .toColorInt()
}