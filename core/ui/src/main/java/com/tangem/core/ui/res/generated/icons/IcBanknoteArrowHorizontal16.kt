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

private var _ic_banknote_arrow_horizontal_16: ImageVector? = null

val Icons.ic_banknote_arrow_horizontal_16: ImageVector
    get() {
        if (_ic_banknote_arrow_horizontal_16 != null) return _ic_banknote_arrow_horizontal_16!!
        _ic_banknote_arrow_horizontal_16 = ImageVector.Builder(
            name = "ic_banknote_arrow_horizontal_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M6.93945 6.94144C7.52517 6.35745 8.47485 6.35687 9.06052 6.94144L9.16306 7.05472C9.64404 7.64297 9.60961 8.51122 9.06052 9.05959C8.47504 9.64321 7.52609 9.6434 6.94042 9.06057C6.35394 8.47575 6.3537 7.52641 6.93945 6.94144ZM8.13768 7.79398C8.05461 7.73951 7.94575 7.73931 7.86229 7.79398L7.82225 7.8262C7.7257 7.92304 7.72613 8.07876 7.82225 8.17483L7.86132 8.20706C7.95893 8.27109 8.09242 8.25976 8.17772 8.17483C8.2621 8.09019 8.27189 7.96067 8.20897 7.86526L8.17772 7.8262L8.13768 7.79398Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M11.5 4.37506C12.3345 4.37506 12.9999 5.05621 12.9999 5.88384V10.1162C12.9999 10.9439 12.3345 11.625 11.5 11.625H4.50002C3.66552 11.6249 3.00003 10.9439 3.00003 10.1162V5.88384C3.0001 5.05625 3.66556 4.37513 4.50002 4.37506H11.5ZM4.50002 5.62505C4.36756 5.62512 4.25008 5.73489 4.25002 5.88384V10.1162C4.25002 10.2652 4.36752 10.3749 4.50002 10.375H11.5C11.6325 10.375 11.75 10.2653 11.75 10.1162V5.88384L11.7451 5.83013C11.7209 5.70918 11.616 5.62505 11.5 5.62505H4.50002Z"),
            )
        }.build()
        return _ic_banknote_arrow_horizontal_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBanknoteArrowHorizontal16Preview() {
    Icon(
        imageVector = Icons.ic_banknote_arrow_horizontal_16,
        contentDescription = null,
    )
}