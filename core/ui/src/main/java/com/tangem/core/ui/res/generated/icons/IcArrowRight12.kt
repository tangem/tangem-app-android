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

private var _ic_arrow_right_12: ImageVector? = null

val Icons.ic_arrow_right_12: ImageVector
    get() {
        if (_ic_arrow_right_12 != null) return _ic_arrow_right_12!!
        _ic_arrow_right_12 = ImageVector.Builder(
            name = "ic_arrow_right_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.64418 2.14284C6.83946 1.9481 7.15611 1.94781 7.35121 2.14284L10.8522 5.64381C11.0469 5.83894 11.0468 6.15566 10.8522 6.35084L7.35121 9.85182C7.15605 10.0468 6.8394 10.0467 6.64418 9.85182C6.44912 9.65662 6.44917 9.34002 6.64418 9.14479L9.29164 6.49733H1.50063C1.22468 6.49733 1.00095 6.27319 1.00063 5.99733C1.00093 5.72145 1.22467 5.49733 1.50063 5.49733H9.29164L6.64418 2.84987C6.4491 2.65471 6.44928 2.3381 6.64418 2.14284Z"),
            )
        }.build()
        return _ic_arrow_right_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRight12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_right_12,
        contentDescription = null,
    )
}