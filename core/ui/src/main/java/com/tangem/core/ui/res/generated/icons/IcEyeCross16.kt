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

private var _ic_eye_cross_16: ImageVector? = null

val Icons.ic_eye_cross_16: ImageVector
    get() {
        if (_ic_eye_cross_16 != null) return _ic_eye_cross_16!!
        _ic_eye_cross_16 = ImageVector.Builder(
            name = "ic_eye_cross_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.06918 2.72895C3.31326 2.48488 3.70987 2.48488 3.95395 2.72895L13.6122 12.3881C13.8561 12.6322 13.8562 13.0279 13.6122 13.2719C13.3681 13.5158 12.9724 13.5158 12.7284 13.2719L11.5702 12.1137C10.5073 12.7948 9.26748 13.1586 7.9979 13.1547C5.60091 13.1923 3.38711 11.8728 2.28012 9.74556L2.27426 9.73384C1.82948 8.83185 1.82931 7.77346 2.27426 6.87153L2.27622 6.86763C2.71367 5.99656 3.35582 5.24959 4.14145 4.68599L3.06918 3.61372C2.82513 3.36967 2.82518 2.97304 3.06918 2.72895ZM5.04086 5.5854C4.34115 6.04272 3.77087 6.67668 3.3934 7.42817C3.12226 7.98062 3.12324 8.62802 3.39536 9.18013C4.28747 10.8837 6.06566 11.9388 7.98911 11.9047L8.0018 11.9038C8.93759 11.9066 9.85309 11.6632 10.659 11.2036L9.59946 10.143C9.05055 10.6284 8.29984 10.852 7.55258 10.7172C6.55124 10.5365 5.76737 9.75184 5.58676 8.75044C5.45217 8.00334 5.67407 7.25213 6.15903 6.70356L5.04086 5.5854ZM7.04868 7.59224C6.84672 7.85306 6.75548 8.19115 6.81625 8.52876C6.90422 9.01648 7.28662 9.39858 7.77426 9.48677C8.11177 9.54764 8.44895 9.45589 8.70981 9.25434L7.04868 7.59224Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.98911 3.45063C10.316 3.40922 12.4713 4.64692 13.6122 6.66157L13.7196 6.85981L13.7264 6.87153C14.1712 7.77302 14.1696 8.8292 13.7254 9.73091L13.7264 9.73188C13.6987 9.78844 13.6685 9.83559 13.6551 9.85688C13.6455 9.87219 13.6413 9.87941 13.6395 9.88227C13.4782 10.1875 13.1 10.3043 12.7948 10.143C12.4898 9.98167 12.3728 9.60339 12.534 9.29829C12.5479 9.27202 12.5615 9.24925 12.5721 9.23188C12.582 9.21568 12.5925 9.19887 12.5975 9.19087C12.6114 9.16882 12.6091 9.17234 12.6043 9.18208L12.6053 9.18013C12.8774 8.62832 12.8779 7.98145 12.6073 7.42915C11.7159 5.72292 9.93676 4.66645 8.01157 4.70063H7.99985C7.88754 4.70067 7.83558 4.71311 7.6395 4.72602C7.29527 4.74859 6.99733 4.48817 6.97446 4.14399C6.95184 3.79966 7.21315 3.50171 7.55747 3.47895C7.62751 3.47433 7.83169 3.45169 7.98911 3.45063Z"),
            )
        }.build()
        return _ic_eye_cross_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcEyeCross16Preview() {
    Icon(
        imageVector = Icons.ic_eye_cross_16,
        contentDescription = null,
    )
}