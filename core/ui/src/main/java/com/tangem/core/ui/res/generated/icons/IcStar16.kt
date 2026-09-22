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

private var _ic_star_16: ImageVector? = null

val Icons.ic_star_16: ImageVector
    get() {
        if (_ic_star_16 != null) return _ic_star_16!!
        _ic_star_16 = ImageVector.Builder(
            name = "ic_star_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.00062 2.28027C8.55937 2.2805 9.06975 2.59677 9.31898 3.09668L10.3805 5.21484L12.7311 5.55078C13.2908 5.62927 13.7572 6.02155 13.9303 6.55957C14.1032 7.09721 13.9528 7.68507 13.5446 8.0752L13.5455 8.07617L11.858 9.69336L12.2594 11.9883L12.2809 12.1953C12.2975 12.6778 12.0758 13.1438 11.6793 13.4346C11.2263 13.7667 10.6236 13.8126 10.1256 13.5527L7.99964 12.4531L5.87367 13.5508L5.87464 13.5518C5.37662 13.8117 4.77399 13.7658 4.32093 13.4336C3.8679 13.1014 3.64263 12.5405 3.74085 11.9873L4.14124 9.69336L2.45472 8.07617L2.4557 8.0752C2.04739 7.68506 1.897 7.09725 2.06996 6.55957C2.22149 6.0887 2.59653 5.72955 3.06312 5.59473L3.2682 5.55078L5.61878 5.21484L6.68031 3.09668C6.92956 2.59639 7.4415 2.28027 8.00062 2.28027ZM8.00062 3.53027C7.91567 3.53027 7.83731 3.57828 7.79945 3.6543L7.79847 3.65723L6.58949 6.06738C6.49773 6.25043 6.32243 6.37719 6.11976 6.40625L3.443 6.78906C3.35796 6.80098 3.28572 6.86064 3.25941 6.94238C3.23314 7.02408 3.25594 7.11359 3.318 7.17285L3.31898 7.17383L5.24671 9.02148C5.39698 9.16554 5.46608 9.37501 5.43031 9.58008L4.9723 12.2041V12.2061C4.95738 12.2901 4.99136 12.3753 5.06019 12.4258C5.12895 12.4761 5.21997 12.4827 5.29554 12.4434L5.29847 12.4424L7.71351 11.1953L7.78187 11.165C7.94583 11.1039 8.12924 11.114 8.28675 11.1953L10.7018 12.4434L10.7047 12.4443C10.7802 12.4835 10.8714 12.4769 10.9401 12.4268C11.0088 12.3763 11.0428 12.291 11.028 12.207V12.2051L10.57 9.58008C10.5342 9.37498 10.6032 9.16555 10.7535 9.02148L12.6813 7.17383L12.6823 7.17285C12.7443 7.1136 12.7671 7.02405 12.7409 6.94238C12.7146 6.86066 12.6432 6.80102 12.5582 6.78906L9.8805 6.40625C9.6778 6.37723 9.50256 6.25044 9.41078 6.06738L8.20179 3.65723L8.20081 3.6543C8.16305 3.57843 8.08535 3.5305 8.00062 3.53027Z"),
            )
        }.build()
        return _ic_star_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcStar16Preview() {
    Icon(
        imageVector = Icons.ic_star_16,
        contentDescription = null,
    )
}