@file:Suppress("TooManyFunctions")

package com.tangem.tap.common.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

fun Context.getDrawableCompat(@DrawableRes drawableResId: Int): Drawable? {
    return ContextCompat.getDrawable(this, drawableResId)
}

@ColorInt
fun Context.getColorCompat(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(this, colorRes)
}

@ColorInt
fun View.getColor(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(context, colorRes)
}

fun View.getString(@StringRes id: Int): String {
    return context.getString(id)
}

fun View.getString(@StringRes id: Int, vararg formatArgs: String): String {
    return context.getString(id, *formatArgs)
}

fun View.show(show: Boolean, invokeBeforeStateChanged: (() -> Unit)? = null) {
    return if (show) this.show(invokeBeforeStateChanged) else this.hide(invokeBeforeStateChanged)
}

fun View.show(invokeBeforeStateChanged: (() -> Unit)? = null) {
    if (this.visibility == View.VISIBLE) return

    invokeBeforeStateChanged?.invoke()
    this.visibility = View.VISIBLE
}

fun View.hide(invokeBeforeStateChanged: (() -> Unit)? = null) {
    if (this.visibility == View.GONE) return

    invokeBeforeStateChanged?.invoke()
    this.visibility = View.GONE
}

fun View.getString(resId: Int, vararg formatArgs: Any?): String {
    return context.getString(resId, *formatArgs)
}