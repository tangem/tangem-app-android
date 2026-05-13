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

private var _ic_loading_spinner_16: ImageVector? = null

val Icons.ic_loading_spinner_16: ImageVector
    get() {
        if (_ic_loading_spinner_16 != null) return _ic_loading_spinner_16!!
        _ic_loading_spinner_16 = ImageVector.Builder(
            name = "ic_loading_spinner_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 2C9.18663 2.00007 10.3473 2.35246 11.334 3.01172C12.3204 3.67102 13.0899 4.60793 13.5439 5.7041C13.9979 6.80037 14.1172 8.00714 13.8857 9.1709C13.6542 10.3347 13.0823 11.4041 12.2432 12.2432C11.4041 13.0823 10.3347 13.6542 9.1709 13.8857C8.00714 14.1172 6.80037 13.9979 5.7041 13.5439C4.60783 13.0899 3.67005 12.3205 3.01074 11.334C2.35155 10.3474 2.00006 9.18657 2 8C2.00016 7.65496 2.27992 7.375 2.625 7.375C2.96992 7.37518 3.24984 7.65507 3.25 8C3.25006 8.93941 3.52887 9.85856 4.05078 10.6396C4.57273 11.4205 5.31485 12.0292 6.18262 12.3887C7.05043 12.748 8.00552 12.8424 8.92676 12.6592C9.84813 12.4759 10.6951 12.0237 11.3594 11.3594C12.0237 10.6951 12.4759 9.84813 12.6592 8.92676C12.8424 8.00552 12.748 7.05043 12.3887 6.18262C12.0292 5.31484 11.4205 4.57273 10.6396 4.05078C9.85856 3.52887 8.93941 3.25007 8 3.25C7.65508 3.24983 7.37518 2.96992 7.375 2.625C7.375 2.27993 7.65496 2.00017 8 2Z"),
            )
        }.build()
        return _ic_loading_spinner_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLoadingSpinner16Preview() {
    Icon(
        imageVector = Icons.ic_loading_spinner_16,
        contentDescription = null,
    )
}