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

private var _ic_card_plus_24: ImageVector? = null

val Icons.ic_card_plus_24: ImageVector
    get() {
        if (_ic_card_plus_24 != null) return _ic_card_plus_24!!
        _ic_card_plus_24 = ImageVector.Builder(
            name = "ic_card_plus_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13 4.4541C13.5522 4.4541 13.9998 4.90195 14 5.4541C14 6.00639 13.5523 6.4541 13 6.4541H6C4.86632 6.4541 4.00017 7.3378 4 8.36328V17.0908C4.00011 18.1164 4.86629 19 6 19H18C19.1337 19 19.9999 18.1164 20 17.0908V11.2725C20.0002 10.7203 20.4478 10.2725 21 10.2725C21.5522 10.2725 21.9998 10.7203 22 11.2725V17.0908C21.9999 19.2785 20.1799 21 18 21H6C3.82014 21 2.00011 19.2785 2 17.0908V8.36328C2.00017 6.1757 3.82018 4.4541 6 4.4541H13Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.57031 15.1221C10.1225 15.1222 10.5703 15.5699 10.5703 16.1221C10.57 16.6739 10.1222 17.1219 9.57031 17.1221H7C6.44794 17.1221 6.00036 16.674 6 16.1221C6 15.5698 6.44772 15.1221 7 15.1221H9.57031Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.5 3C20.0523 3 20.5 3.44772 20.5 4V4.45508H21C21.5523 4.45508 22 4.90279 22 5.45508C21.9997 6.00714 21.5521 6.45508 21 6.45508H20.5V6.91016C20.4999 7.46237 20.0522 7.91016 19.5 7.91016C18.9478 7.91016 18.5001 7.46237 18.5 6.91016V6.45508H18C17.4479 6.45508 17.0003 6.00714 17 5.45508C17 4.90279 17.4477 4.45508 18 4.45508H18.5V4C18.5 3.44772 18.9477 3 19.5 3Z"),
            )
        }.build()
        return _ic_card_plus_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCardPlus24Preview() {
    Icon(
        imageVector = Icons.ic_card_plus_24,
        contentDescription = null,
    )
}