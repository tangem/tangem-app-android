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

private var _ic_heart_28_filled: ImageVector? = null

val Icons.ic_heart_28_filled: ImageVector
    get() {
        if (_ic_heart_28_filled != null) return _ic_heart_28_filled!!
        _ic_heart_28_filled = ImageVector.Builder(
            name = "ic_heart_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.9328 3.49902C23.1595 3.49902 25.9995 7.41027 25.9995 11.059C25.9995 18.4484 14.2128 24.499 13.9995 24.499C13.7862 24.499 1.99951 18.4484 1.99951 11.059C1.99951 7.41027 4.83951 3.49902 9.06618 3.49902C11.4928 3.49902 13.0795 4.6934 13.9995 5.7434C14.9195 4.6934 16.5062 3.49902 18.9328 3.49902Z"),
            )
        }.build()
        return _ic_heart_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeart28FilledPreview() {
    Icon(
        imageVector = Icons.ic_heart_28_filled,
        contentDescription = null,
    )
}