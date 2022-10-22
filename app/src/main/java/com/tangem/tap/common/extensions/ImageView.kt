package com.tangem.tap.common.extensions

import android.widget.ImageView
import androidx.annotation.DrawableRes

/**
[REDACTED_AUTHOR]
 */
fun ImageView.setDrawable(@DrawableRes resId: Int) {
    setImageDrawable(context.getDrawableCompat(resId))
}