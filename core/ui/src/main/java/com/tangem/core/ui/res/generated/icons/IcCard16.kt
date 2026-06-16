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

private var _ic_card_16: ImageVector? = null

val Icons.ic_card_16: ImageVector
    get() {
        if (_ic_card_16 != null) return _ic_card_16!!
        _ic_card_16 = ImageVector.Builder(
            name = "ic_card_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.08691 9.13184C6.43135 9.13235 6.71142 9.4124 6.71191 9.75684C6.71191 10.1017 6.43165 10.3813 6.08691 10.3818H4.47559C4.13041 10.3818 3.85059 10.102 3.85059 9.75684C3.85109 9.41208 4.13072 9.13184 4.47559 9.13184H6.08691Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.9189 3C13.3199 3.00027 14.5017 4.09489 14.502 5.50098V10.5029C14.502 11.9092 13.32 13.0036 11.9189 13.0039H4.08398C2.68267 13.0039 1.5 11.9094 1.5 10.5029V5.50098C1.50024 4.09472 2.68282 3 4.08398 3H11.9189ZM2.75 10.5029C2.75 11.1687 3.32155 11.7539 4.08398 11.7539H11.9189C12.6811 11.7536 13.252 11.1685 13.252 10.5029V6.85254H2.75V10.5029ZM4.08398 4.25C3.32171 4.25 2.75025 4.83543 2.75 5.50098V5.60254H13.252V5.50098C13.2517 4.83558 12.681 4.25027 11.9189 4.25H4.08398Z"),
            )
        }.build()
        return _ic_card_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCard16Preview() {
    Icon(
        imageVector = Icons.ic_card_16,
        contentDescription = null,
    )
}