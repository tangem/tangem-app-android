@file:Suppress("all")

package com.tangem.core.ui.res.generated.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _ic_unlock_20_filled: ImageVector? = null

val Icons.ic_unlock_20_filled: ImageVector
    get() {
        if (_ic_unlock_20_filled != null) return _ic_unlock_20_filled!!
        _ic_unlock_20_filled = ImageVector.Builder(
            name = "ic_unlock_20_filled",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 2C12.0832 2 13.75 3.71506 13.75 5.80273V7.3418C15.0052 7.3418 16 8.37301 16 9.61816V15.7236C15.9999 16.9687 15.0051 18 13.75 18H6.25C4.99488 18 4.00014 16.9687 4 15.7236V9.61816C4 8.37301 4.99479 7.3418 6.25 7.3418H12.25V5.80273C12.25 4.51878 11.2303 3.5 10 3.5C8.96203 3.50013 8.07516 4.22396 7.82227 5.22363C7.72065 5.62501 7.31255 5.86796 6.91113 5.7666C6.50964 5.66498 6.26565 5.25698 6.36719 4.85547C6.78037 3.2222 8.24267 2.00013 10 2Z"),
            )
        }.build()
        return _ic_unlock_20_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUnlock20FilledPreview() {
    Icon(
        imageVector = Icons.ic_unlock_20_filled,
        contentDescription = null,
    )
}