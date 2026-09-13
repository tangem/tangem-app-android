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

private var _ic_arrow_right_left_16: ImageVector? = null

val Icons.ic_arrow_right_left_16: ImageVector
    get() {
        if (_ic_arrow_right_left_16 != null) return _ic_arrow_right_left_16!!
        _ic_arrow_right_left_16 = ImageVector.Builder(
            name = "ic_arrow_right_left_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.49512 8.12363C5.73927 7.88006 6.13503 7.88064 6.37891 8.12461C6.6225 8.36865 6.6226 8.76445 6.37891 9.0084L5.54102 9.84531H11.9688C12.3137 9.84557 12.5938 10.1253 12.5938 10.4703C12.5937 10.8153 12.3137 11.095 11.9688 11.0953H5.54199L6.37891 11.9332C6.62264 12.1772 6.6226 12.573 6.37891 12.817C6.1349 13.0609 5.73919 13.0608 5.49512 12.817L3.58984 10.9117C3.47293 10.7946 3.40629 10.6358 3.40625 10.4703C3.40629 10.345 3.44452 10.2232 3.51367 10.1207C3.53746 10.0855 3.56446 10.0526 3.59473 10.023L5.49512 8.12363Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.62207 3.1832C9.86615 2.93941 10.2619 2.93927 10.5059 3.1832L12.4043 5.08164C12.4925 5.16766 12.5548 5.27939 12.5801 5.40488C12.5884 5.44572 12.5937 5.48758 12.5938 5.52988C12.5937 5.69553 12.5273 5.85511 12.4102 5.97226L10.5059 7.87656C10.2618 8.12034 9.86608 8.11935 9.62207 7.87558C9.3782 7.63145 9.378 7.23578 9.62207 6.9918L10.4609 6.1539H4.03223C3.68709 6.1539 3.40631 5.87403 3.40625 5.5289C3.40625 5.18373 3.68705 4.9039 4.03223 4.9039H10.458L9.62207 4.06699C9.37805 3.82291 9.37801 3.42726 9.62207 3.1832Z"),
            )
        }.build()
        return _ic_arrow_right_left_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRightLeft16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_right_left_16,
        contentDescription = null,
    )
}