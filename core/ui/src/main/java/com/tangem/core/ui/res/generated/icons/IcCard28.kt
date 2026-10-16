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

private var _ic_card_28: ImageVector? = null

val Icons.ic_card_28: ImageVector
    get() {
        if (_ic_card_28 != null) return _ic_card_28!!
        _ic_card_28 = ImageVector.Builder(
            name = "ic_card_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.0977 16.2852C11.7876 16.2853 12.3472 16.8452 12.3477 17.5352C12.3477 18.2254 11.7879 18.785 11.0977 18.7852H8.02832C7.33796 18.7852 6.77832 18.2255 6.77832 17.5352C6.77874 16.8452 7.33823 16.2852 8.02832 16.2852H11.0977Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.167 4.5C23.8206 4.50017 26 6.62728 26 9.28613V18.7139C26 21.3727 23.8206 23.4998 21.167 23.5H6.83301C4.17943 23.4998 2 21.3727 2 18.7139V9.28613C2 6.62728 4.17943 4.50017 6.83301 4.5H21.167ZM4.5 18.7139C4.5 19.9606 5.5285 20.9998 6.83301 21H21.167C22.4715 20.9998 23.5 19.9606 23.5 18.7139V11.7139H4.5V18.7139ZM6.83301 7C5.55382 7.00017 4.54278 7.99956 4.50391 9.21387H23.4961C23.4572 7.99956 22.4462 7.00017 21.167 7H6.83301Z"),
            )
        }.build()
        return _ic_card_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCard28Preview() {
    Icon(
        imageVector = Icons.ic_card_28,
        contentDescription = null,
    )
}