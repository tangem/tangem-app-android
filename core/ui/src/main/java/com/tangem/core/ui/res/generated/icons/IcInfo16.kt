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

private var _ic_info_16: ImageVector? = null

val Icons.ic_info_16: ImageVector
    get() {
        if (_ic_info_16 != null) return _ic_info_16!!
        _ic_info_16 = ImageVector.Builder(
            name = "ic_info_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 7.375C8.34502 7.375 8.62474 7.65505 8.625 8V10.6875C8.625 11.0327 8.34518 11.3125 8 11.3125C7.6552 11.3121 7.375 11.0324 7.375 10.6875V8.625H7.10449C6.75948 8.6248 6.47949 8.34506 6.47949 8C6.47976 7.65517 6.75964 7.3752 7.10449 7.375H8Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.93066 5.28906C8.32023 5.32876 8.62588 5.65732 8.62598 6.05957C8.62594 6.48724 8.27925 6.83398 7.85156 6.83398C7.45073 6.83373 7.12059 6.52915 7.08105 6.13867L7.07715 6.05957C7.07659 5.62976 7.42643 5.28532 7.85156 5.28516L7.93066 5.28906Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.00098 2C11.3154 2.00008 14.002 4.68658 14.002 8.00098C14.0017 11.3151 11.3152 14.0019 8.00098 14.002C4.6867 14.002 2.00027 11.3152 2 8.00098C2 4.68653 4.68653 2 8.00098 2ZM8.00098 3.25C5.37689 3.25 3.25 5.37689 3.25 8.00098C3.25027 10.6248 5.37705 12.752 8.00098 12.752C10.6248 12.7519 12.7517 10.6248 12.752 8.00098C12.752 5.37693 10.625 3.25008 8.00098 3.25Z"),
            )
        }.build()
        return _ic_info_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcInfo16Preview() {
    Icon(
        imageVector = Icons.ic_info_16,
        contentDescription = null,
    )
}