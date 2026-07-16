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

private var _ic_card_12: ImageVector? = null

val Icons.ic_card_12: ImageVector
    get() {
        if (_ic_card_12 != null) return _ic_card_12!!
        _ic_card_12 = ImageVector.Builder(
            name = "ic_card_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.5 6.9375C4.77605 6.93761 5 7.16142 5 7.4375C4.99969 7.71332 4.77586 7.93739 4.5 7.9375H3.2998C3.02385 7.9375 2.80011 7.71338 2.7998 7.4375C2.7998 7.16136 3.02366 6.9375 3.2998 6.9375H4.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9 2C10.1046 2 11 2.89536 11 4V8C11 9.10464 10.1046 10 9 10H3C1.89536 10 1 9.10464 1 8V4C1 2.89536 1.89536 2 3 2H9ZM2 8C2 8.55236 2.44764 9 3 9H9C9.55236 9 10 8.55236 10 8V5H2V8ZM3 3C2.44764 3 2 3.44764 2 4H10C10 3.44764 9.55236 3 9 3H3Z"),
            )
        }.build()
        return _ic_card_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCard12Preview() {
    Icon(
        imageVector = Icons.ic_card_12,
        contentDescription = null,
    )
}