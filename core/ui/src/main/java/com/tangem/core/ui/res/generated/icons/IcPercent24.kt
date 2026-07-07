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

private var _ic_percent_24: ImageVector? = null

val Icons.ic_percent_24: ImageVector
    get() {
        if (_ic_percent_24 != null) return _ic_percent_24!!
        _ic_percent_24 = ImageVector.Builder(
            name = "ic_percent_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.1704 15.1704C16.7324 13.6084 19.2647 13.6084 20.8267 15.1704L20.9683 15.3188C22.3869 16.8889 22.3398 19.3134 20.8267 20.8266C19.2648 22.3885 16.7324 22.3882 15.1704 20.8266C13.6084 19.2647 13.6085 16.7324 15.1704 15.1704ZM19.2603 16.4467C18.4748 15.8062 17.3165 15.8524 16.5845 16.5844C15.8036 17.3654 15.8036 18.6316 16.5845 19.4126C17.3654 20.193 18.6318 20.1933 19.4126 19.4126C20.1447 18.6804 20.19 17.5212 19.5493 16.7358L19.4126 16.5844L19.2603 16.4467Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.7905 3.79245C19.1809 3.40203 19.814 3.40224 20.2046 3.79245C20.5951 4.18297 20.5951 4.81598 20.2046 5.20651L5.20654 20.2046C4.81601 20.5949 4.18295 20.595 3.79248 20.2046C3.40239 19.8141 3.40225 19.1809 3.79248 18.7905L18.7905 3.79245Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.17139 3.17135C4.73327 1.60947 7.26565 1.60963 8.82764 3.17135L8.96924 3.31979C10.388 4.88987 10.3408 7.31437 8.82764 8.8276C7.26577 10.3893 4.73333 10.3892 3.17139 8.8276C1.60972 7.26565 1.60964 4.73326 3.17139 3.17135ZM7.26123 4.44772C6.47582 3.80747 5.31743 3.85343 4.58545 4.58542C3.80474 5.36627 3.80483 6.63264 4.58545 7.41354C5.36635 8.19411 6.63275 8.19423 7.41357 7.41354C8.14573 6.68135 8.19107 5.5222 7.55029 4.73678L7.41357 4.58542L7.26123 4.44772Z"),
            )
        }.build()
        return _ic_percent_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPercent24Preview() {
    Icon(
        imageVector = Icons.ic_percent_24,
        contentDescription = null,
    )
}