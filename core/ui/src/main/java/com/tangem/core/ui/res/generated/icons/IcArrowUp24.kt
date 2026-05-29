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

private var _ic_arrow_up_24: ImageVector? = null

val Icons.ic_arrow_up_24: ImageVector
    get() {
        if (_ic_arrow_up_24 != null) return _ic_arrow_up_24!!
        _ic_arrow_up_24 = ImageVector.Builder(
            name = "ic_arrow_up_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.7048 9.29199C20.0948 9.68237 20.0947 10.3156 19.7048 10.7061C19.3144 11.0963 18.6813 11.0961 18.2908 10.7061L12.9988 5.41406L12.9988 20.999C12.9986 21.5509 12.5506 21.9986 11.9988 21.999C11.4466 21.999 10.9989 21.5512 10.9988 20.999L10.9988 5.41406L5.70679 10.7061C5.31639 11.0963 4.68324 11.0961 4.29272 10.7061C3.9024 10.3156 3.90237 9.68248 4.29272 9.29199L11.2917 2.29297C11.4792 2.10579 11.7338 2.00001 11.9988 2C12.2637 2.00021 12.5185 2.1057 12.7058 2.29297L19.7048 9.29199Z"),
            )
        }.build()
        return _ic_arrow_up_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUp24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_24,
        contentDescription = null,
    )
}