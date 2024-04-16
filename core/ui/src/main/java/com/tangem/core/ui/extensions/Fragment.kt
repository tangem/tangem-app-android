package com.tangem.core.ui.extensions

import android.R
import android.content.Context
import android.graphics.Color.*
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import kotlin.math.sqrt

@Deprecated("Use only in legacy fragments")
fun Fragment.setStatusBarColor(@ColorRes colorResId: Int) {
    with(requireActivity().window) {
        clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        statusBarColor = ContextCompat.getColor(requireContext(), colorResId)
        val view = view ?: return
        val windowInsetsController = WindowCompat.getInsetsController(this, view)
        windowInsetsController.isAppearanceLightStatusBars = luminance(requireContext(), colorResId)
    }
}

// TODO replace by android.graphics.luminance() after bump min API to 24
@Suppress("MagicNumber")
fun luminance(context: Context, @ColorRes colorRes: Int): Boolean {
    val color = context.resources.getColor(colorRes, null)
    if (R.color.transparent == color) return true
    var rtnValue = false
    val rgb = intArrayOf(red(color), green(color), blue(color))
    val brightness = sqrt(
        rgb[0] * rgb[0] * .241 +
            rgb[1] * rgb[1] * .691 +
            rgb[2] * rgb[2] * .068,
    ).toInt()

    // color is light
    if (brightness >= 200) {
        rtnValue = true
    }
    return rtnValue
}