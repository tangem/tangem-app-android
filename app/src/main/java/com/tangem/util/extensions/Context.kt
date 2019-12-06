package com.tangem.util.extensions

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun Context.colorFrom(@ColorRes colorId: Int) = ContextCompat.getColor(this, colorId)