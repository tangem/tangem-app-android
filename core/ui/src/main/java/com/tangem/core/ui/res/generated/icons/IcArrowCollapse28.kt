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

private var _ic_arrow_collapse_28: ImageVector? = null

val Icons.ic_arrow_collapse_28: ImageVector
    get() {
        if (_ic_arrow_collapse_28 != null) return _ic_arrow_collapse_28!!
        _ic_arrow_collapse_28 = ImageVector.Builder(
            name = "ic_arrow_collapse_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.3086 14.3057C13.0738 14.3058 13.6941 14.9262 13.6943 15.6914V20.7412C13.694 21.4313 13.1344 21.9912 12.4443 21.9912C11.7542 21.9912 11.1946 21.4313 11.1943 20.7412V18.5732L7.88376 21.8838C7.39564 22.3718 6.60432 22.3718 6.11618 21.8838C5.6281 21.3957 5.62811 20.6044 6.11618 20.1162L9.42673 16.8057H7.25876C6.56868 16.8054 6.00886 16.2458 6.00876 15.5557C6.00876 14.8655 6.56862 14.3059 7.25876 14.3057H12.3086Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.1162 6.11622C20.6043 5.62811 21.3956 5.62808 21.8838 6.11622C22.3718 6.60436 22.3718 7.39566 21.8838 7.88379L18.5732 11.1943H20.7412C21.4313 11.1946 21.9912 11.7542 21.9912 12.4443C21.9912 13.1345 21.4313 13.6941 20.7412 13.6943H15.6914C14.9261 13.6942 14.3058 13.0739 14.3056 12.3086V7.25879C14.3058 6.56862 14.8654 6.00879 15.5556 6.00879C16.2458 6.00889 16.8054 6.56867 16.8056 7.25879V9.42676L20.1162 6.11622Z"),
            )
        }.build()
        return _ic_arrow_collapse_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowCollapse28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_collapse_28,
        contentDescription = null,
    )
}