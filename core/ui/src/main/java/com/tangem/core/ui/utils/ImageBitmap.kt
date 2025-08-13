package com.tangem.core.ui.utils

import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

/**
[REDACTED_AUTHOR]
 */
@Composable
fun asImageBitmap(@DrawableRes drawableId: Int): ImageBitmap {
    val drawable = requireNotNull(AppCompatResources.getDrawable(LocalContext.current, drawableId)) {
        "drawable is null"
    }
    return drawable.toBitmap().asImageBitmap()
}