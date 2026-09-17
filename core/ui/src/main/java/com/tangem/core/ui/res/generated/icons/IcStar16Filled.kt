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

private var _ic_star_16_filled: ImageVector? = null

val Icons.ic_star_16_filled: ImageVector
    get() {
        if (_ic_star_16_filled != null) return _ic_star_16_filled!!
        _ic_star_16_filled = ImageVector.Builder(
            name = "ic_star_16_filled",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.00062 2.28027C8.55937 2.2805 9.06975 2.59677 9.31898 3.09668L10.3805 5.21484L12.7311 5.55078C13.2908 5.62927 13.7572 6.02155 13.9303 6.55957C14.1032 7.09721 13.9528 7.68507 13.5446 8.0752L13.5455 8.07617L11.858 9.69336L12.2594 11.9883L12.2809 12.1953C12.2975 12.6778 12.0758 13.1438 11.6793 13.4346C11.2263 13.7667 10.6236 13.8126 10.1256 13.5527L7.99964 12.4531L5.87367 13.5508L5.87464 13.5518C5.37662 13.8117 4.77399 13.7658 4.32093 13.4336C3.8679 13.1014 3.64263 12.5405 3.74085 11.9873L4.14124 9.69336L2.45472 8.07617L2.4557 8.0752C2.04739 7.68506 1.897 7.09725 2.06996 6.55957C2.22149 6.0887 2.59653 5.72955 3.06312 5.59473L3.2682 5.55078L5.61878 5.21484L6.68031 3.09668C6.92956 2.59639 7.4415 2.28027 8.00062 2.28027Z"),
            )
        }.build()
        return _ic_star_16_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcStar16FilledPreview() {
    Icon(
        imageVector = Icons.ic_star_16_filled,
        contentDescription = null,
    )
}