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

private var _ic_perps_16: ImageVector? = null

val Icons.ic_perps_16: ImageVector
    get() {
        if (_ic_perps_16 != null) return _ic_perps_16!!
        _ic_perps_16 = ImageVector.Builder(
            name = "ic_perps_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.43164 6.68359C9.15837 5.95686 10.3368 5.95752 11.0635 6.68457L11.0645 6.68359C11.7915 7.41065 11.7915 8.58935 11.0645 9.31641C10.3828 9.99802 9.30443 10.0408 8.57324 9.44434L8.43164 9.31641L8 8.88379L7.56836 9.31641C6.88712 9.99764 5.80811 10.0405 5.07715 9.44434L4.93555 9.31641C4.20871 8.58933 4.20856 7.41058 4.93555 6.68359C5.66253 5.95661 6.84129 5.95676 7.56836 6.68359L7.99902 7.11523L8.43164 6.68359ZM6.68457 7.56738C6.44565 7.3287 6.05816 7.32856 5.81934 7.56738C5.58051 7.80621 5.58066 8.1937 5.81934 8.43262C6.05747 8.67051 6.44568 8.6715 6.68457 8.43262L7.11621 8L6.68457 7.56738ZM10.1807 7.56738C9.94273 7.32915 9.55549 7.32879 9.31641 7.56738L8.88379 8L9.31641 8.43262L9.41211 8.51074C9.64947 8.66749 9.97164 8.64162 10.1807 8.43262C10.3897 8.22359 10.4155 7.90143 10.2588 7.66406L10.1807 7.56738Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 2.00195C11.3126 2.00195 13.998 4.68737 13.998 8C13.998 11.3126 11.3126 13.998 8 13.998C4.68737 13.998 2.00195 11.3126 2.00195 8C2.00195 4.68737 4.68737 2.00195 8 2.00195ZM8 3.25195C5.37773 3.25195 3.25195 5.37773 3.25195 8C3.25195 10.6223 5.37773 12.748 8 12.748C10.6223 12.748 12.748 10.6223 12.748 8C12.748 5.37773 10.6223 3.25195 8 3.25195Z"),
            )
        }.build()
        return _ic_perps_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPerps16Preview() {
    Icon(
        imageVector = Icons.ic_perps_16,
        contentDescription = null,
    )
}