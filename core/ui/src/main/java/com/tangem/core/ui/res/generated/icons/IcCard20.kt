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

private var _ic_card_20: ImageVector? = null

val Icons.ic_card_20: ImageVector
    get() {
        if (_ic_card_20 != null) return _ic_card_20!!
        _ic_card_20 = ImageVector.Builder(
            name = "ic_card_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.63965 12.0664C8.05362 12.0667 8.38965 12.4024 8.38965 12.8164C8.38946 13.2303 8.0535 13.5661 7.63965 13.5664H5.65137C5.23727 13.5664 4.90156 13.2305 4.90137 12.8164C4.90137 12.4022 5.23715 12.0664 5.65137 12.0664H7.63965Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.8379 3.5C16.6005 3.50047 18.0049 4.95342 18.0049 6.71484V13.2861C18.0048 15.0475 16.6004 16.5005 14.8379 16.501H5.16797C3.40516 16.5008 2.00005 15.0477 2 13.2861V6.71484C2 4.95324 3.40513 3.50017 5.16797 3.5H14.8379ZM3.50098 13.2861C3.50103 14.2467 4.2608 15.0008 5.16797 15.001H14.8379C15.7448 15.0005 16.5048 14.2466 16.5049 13.2861V8.35449H3.50098V13.2861ZM5.16797 5C4.26077 5.00017 3.50098 5.75419 3.50098 6.71484V6.85449H16.5049V6.71484C16.5049 5.75438 15.7448 5.00048 14.8379 5H5.16797Z"),
            )
        }.build()
        return _ic_card_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCard20Preview() {
    Icon(
        imageVector = Icons.ic_card_20,
        contentDescription = null,
    )
}