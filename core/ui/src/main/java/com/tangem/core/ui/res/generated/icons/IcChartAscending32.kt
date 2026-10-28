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

private var _ic_chart_ascending_32: ImageVector? = null

val Icons.ic_chart_ascending_32: ImageVector
    get() {
        if (_ic_chart_ascending_32 != null) return _ic_chart_ascending_32!!
        _ic_chart_ascending_32 = ImageVector.Builder(
            name = "ic_chart_ascending_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.5 4.5C6.32843 4.5 7 5.17157 7 6V24.5H26.499C27.3273 24.5002 27.999 25.1717 27.999 26C27.999 26.8283 27.3273 27.4998 26.499 27.5H5.5C4.67157 27.5 4 26.8284 4 26V6C4 5.17157 4.67157 4.5 5.5 4.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M26.5654 8.77148C26.5869 8.77243 26.6085 8.77157 26.6299 8.77344C26.6615 8.77615 26.6926 8.78149 26.7236 8.78613C26.7432 8.78907 26.7628 8.79121 26.7822 8.79492C26.8057 8.79939 26.8285 8.80598 26.8516 8.81152C26.8767 8.81759 26.9019 8.82367 26.9268 8.83105C26.9545 8.83929 26.9817 8.84863 27.0088 8.8584C27.0244 8.86402 27.0402 8.86885 27.0557 8.875C27.0849 8.88666 27.1133 8.89968 27.1416 8.91309C27.1596 8.92162 27.1776 8.93017 27.1953 8.93945C27.226 8.95555 27.2558 8.9731 27.2852 8.99121C27.2942 8.9968 27.3035 9.00201 27.3125 9.00781C27.7259 9.27492 28 9.73976 28 10.2686V13.2812C27.9997 14.1093 27.3281 14.7811 26.5 14.7812C25.8701 14.7812 25.3316 14.3921 25.1094 13.8418L21.0127 18.0439L21.0137 18.0449C19.9947 19.0926 18.3259 19.0902 17.3076 18.0459L15.2461 15.9307L9.95996 21.3555C9.38178 21.9483 8.43201 21.9597 7.83887 21.3818C7.24583 20.8036 7.23351 19.8539 7.81152 19.2607L13.3936 13.5342L13.5918 13.3496C14.618 12.4923 16.1452 12.5524 17.1006 13.5352L19.1592 15.6484L23.0361 11.6709C22.4683 11.4574 22.0637 10.911 22.0635 10.2686C22.0635 9.44013 22.735 8.76855 23.5635 8.76855H26.5C26.522 8.76856 26.5437 8.77055 26.5654 8.77148ZM18.8623 15.9531L18.8643 15.9521L18.8672 15.9482C18.8657 15.9498 18.8638 15.9516 18.8623 15.9531Z"),
            )
        }.build()
        return _ic_chart_ascending_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartAscending32Preview() {
    Icon(
        imageVector = Icons.ic_chart_ascending_32,
        contentDescription = null,
    )
}