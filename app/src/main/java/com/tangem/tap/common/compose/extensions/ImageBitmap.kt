package com.tangem.tap.common.compose.extensions

import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

/**
 * Created by Anton Zhilenkov on 08/06/2022.
 */
@Composable
fun asImageBitmap(@DrawableRes drawableId: Int): ImageBitmap {
    val drawable = requireNotNull(AppCompatResources.getDrawable(LocalContext.current, drawableId)) {
        "drawable is null"
    }
    return drawable.toBitmap().asImageBitmap()
}
