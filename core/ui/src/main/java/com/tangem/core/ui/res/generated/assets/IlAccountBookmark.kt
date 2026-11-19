@file:Suppress("all")

package com.tangem.core.ui.res.generated.assets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.generated.icons.Icons

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _il_account_bookmark: ImageVector? = null

val Icons.il_account_bookmark: ImageVector
    get() {
        if (_il_account_bookmark != null) return _il_account_bookmark!!
        _il_account_bookmark = ImageVector.Builder(
            name = "il_account_bookmark",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M28.1295 15C30.267 15.0002 31.9998 16.7268 32 18.8567V31.714L31.9879 31.8927C31.9304 32.3028 31.6758 32.6648 31.2997 32.8576C30.9233 33.0504 30.48 33.0458 30.1116 32.8536L29.9594 32.7602L23.999 28.517L18.0406 32.7592C17.6478 33.0391 17.131 33.0771 16.7013 32.8576C16.2718 32.6379 16.0009 32.1971 16 31.716V18.8567C16.0002 16.7268 17.733 15.0002 19.8705 15H28.1295Z"),
            )
        }.build()
        return _il_account_bookmark!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountBookmarkPreview() {
    Icon(
        imageVector = Icons.il_account_bookmark,
        contentDescription = null,
    )
}