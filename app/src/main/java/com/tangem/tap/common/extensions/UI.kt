package com.tangem.tap.common.extensions

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

@ColorInt
fun Context.getColorCompat(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(this, colorRes)
}