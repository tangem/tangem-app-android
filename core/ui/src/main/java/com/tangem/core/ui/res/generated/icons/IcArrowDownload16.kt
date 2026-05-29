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

private var _ic_arrow_download_16: ImageVector? = null

val Icons.ic_arrow_download_16: ImageVector
    get() {
        if (_ic_arrow_download_16 != null) return _ic_arrow_download_16!!
        _ic_arrow_download_16 = ImageVector.Builder(
            name = "ic_arrow_download_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.3779 12.7451C13.7228 12.7453 14.0027 13.0252 14.0029 13.3701C14.0027 13.715 13.7228 13.9949 13.3779 13.9951H2.625C2.27994 13.9951 2.00018 13.7151 2 13.3701C2.00018 13.0251 2.27993 12.7451 2.625 12.7451H13.3779Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.00098 2C8.34615 2 8.62598 2.27982 8.62598 2.625V9.59082L10.9463 7.53027C11.2044 7.30141 11.6 7.3241 11.8291 7.58203C12.0581 7.83997 12.034 8.23462 11.7764 8.46387L8.41602 11.4502C8.40238 11.4623 8.38548 11.4707 8.37109 11.4814C8.36061 11.4892 8.35082 11.4978 8.33984 11.5049C8.31406 11.5217 8.28723 11.5353 8.25977 11.5479C8.25221 11.5513 8.24502 11.5554 8.2373 11.5586C8.20496 11.5719 8.1724 11.5832 8.13867 11.5908C8.13375 11.5919 8.12899 11.5937 8.12402 11.5947C8.04367 11.6109 7.9613 11.6104 7.88086 11.5947C7.8712 11.5928 7.86203 11.5892 7.85254 11.5869C7.82143 11.5793 7.79062 11.571 7.76074 11.5586C7.75671 11.5569 7.75301 11.5545 7.74902 11.5527C7.71757 11.5389 7.6875 11.5222 7.6582 11.5029C7.6498 11.4974 7.6419 11.4913 7.63379 11.4854C7.61791 11.4737 7.60092 11.4635 7.58594 11.4502L4.22559 8.46387C3.9677 8.23458 3.94461 7.84002 4.17383 7.58203C4.40316 7.3245 4.7978 7.30117 5.05566 7.53027L7.37598 9.59082V2.625C7.37598 2.27985 7.65584 2.00005 8.00098 2Z"),
            )
        }.build()
        return _ic_arrow_download_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownload16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_download_16,
        contentDescription = null,
    )
}