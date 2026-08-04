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

private var _ic_edit_20: ImageVector? = null

val Icons.ic_edit_20: ImageVector
    get() {
        if (_ic_edit_20 != null) return _ic_edit_20!!
        _ic_edit_20 = ImageVector.Builder(
            name = "ic_edit_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 3C10.414 3.00027 10.75 3.33595 10.75 3.75C10.75 4.16402 10.414 4.49973 10 4.5H7.22168C5.71863 4.5002 4.5002 5.71863 4.5 7.22168V12.7773C4.50005 14.2805 5.71853 15.4988 7.22168 15.499H12.7773C14.2806 15.4989 15.499 14.2806 15.499 12.7773V10C15.499 9.5859 15.835 9.25018 16.249 9.25C16.6632 9.2501 16.999 9.58585 16.999 10V12.7773C16.999 15.109 15.109 16.9989 12.7773 16.999H7.22168C4.89011 16.9988 3.00005 15.109 3 12.7773V7.22168C3.0002 4.8902 4.8902 3.0002 7.22168 3H10Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.7041 3C15.3127 3.00063 15.8962 3.24314 16.3262 3.67383C16.757 4.10457 16.999 4.68886 16.999 5.29785C16.999 5.90709 16.7569 6.49135 16.3262 6.92188C16.3189 6.92918 16.3103 6.93545 16.3027 6.94238H16.3047C16.3081 6.93926 16.3135 6.93451 16.3154 6.93262L15.834 7.41406C15.5321 7.7158 15.1182 8.12892 14.6719 8.5752C13.7792 9.46775 12.7549 10.4922 12.2285 11.0186C11.8808 11.3662 11.4265 11.5868 10.9385 11.6465L9.86133 11.7803C9.40623 11.8364 8.95107 11.6769 8.62988 11.3496C8.30873 11.0223 8.15798 10.5643 8.22266 10.1104L8.38184 8.99317C8.44827 8.52187 8.66648 8.08476 9.00293 7.74805C9.73986 7.0108 11.5406 5.20998 13.0791 3.67188C13.51 3.24129 14.0949 2.99946 14.7041 3ZM15.2656 4.7334C15.1166 4.58395 14.9132 4.50024 14.7021 4.5C14.5177 4.49995 14.3398 4.56396 14.1982 4.67969L14.1396 4.73242C12.6013 6.27043 10.8013 8.07145 10.0645 8.80859C9.95776 8.91535 9.8881 9.05365 9.86719 9.20313V9.20606L9.71289 10.2861L10.7549 10.1582H10.7568C10.9123 10.1391 11.0572 10.0687 11.168 9.95801L15.2686 5.85742C15.416 5.70838 15.499 5.50744 15.499 5.29785C15.499 5.08655 15.4149 4.88268 15.2656 4.7334Z"),
            )
        }.build()
        return _ic_edit_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcEdit20Preview() {
    Icon(
        imageVector = Icons.ic_edit_20,
        contentDescription = null,
    )
}