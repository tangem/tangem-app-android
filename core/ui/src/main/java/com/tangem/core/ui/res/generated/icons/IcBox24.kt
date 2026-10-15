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

private var _ic_box_24: ImageVector? = null

val Icons.ic_box_24: ImageVector
    get() {
        if (_ic_box_24 != null) return _ic_box_24!!
        _ic_box_24 = ImageVector.Builder(
            name = "ic_box_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.6079 2.37419C11.4696 1.87722 12.5304 1.87722 13.3921 2.37419L19.6401 5.98063L19.7974 6.07829C20.1524 6.31584 20.442 6.6289 20.6519 6.98844C20.6542 6.99245 20.6573 6.99611 20.6597 7.00016C20.6624 7.00483 20.6639 7.01012 20.6665 7.01481C20.9018 7.42815 21.0327 7.90124 21.0327 8.39274V15.6076C21.0324 16.6025 20.5023 17.5224 19.6401 18.0197L19.6392 18.0187L13.3931 21.6261C12.5854 22.0918 11.6015 22.1206 10.772 21.7131L10.6079 21.6261L4.35986 18.0187C3.49891 17.5207 2.96754 16.6017 2.96729 15.6066V8.39274C2.96729 7.39767 3.49769 6.47808 4.35986 5.98063L7.45654 4.19255C7.46139 4.18955 7.4663 4.18667 7.47119 4.18376L10.6079 2.37419ZM12.9995 12.5763V19.5431L18.6411 16.2863C18.8832 16.1465 19.0324 15.8882 19.0327 15.6076V9.09391L12.9995 12.5763ZM4.96729 15.6066C4.96754 15.8872 5.11735 16.1465 5.36084 16.2873L10.9995 19.5422V12.5754L4.96729 9.09391V15.6066ZM5.96729 7.36149L11.9995 10.8439L14.0161 9.67985L7.98389 6.19743L5.96729 7.36149ZM12.3931 4.10661C12.1498 3.96632 11.8502 3.9654 11.6069 4.10563L11.6079 4.10661L9.98389 5.04313L16.0151 8.52555L18.0317 7.36149L12.3931 4.10661Z"),
            )
        }.build()
        return _ic_box_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBox24Preview() {
    Icon(
        imageVector = Icons.ic_box_24,
        contentDescription = null,
    )
}