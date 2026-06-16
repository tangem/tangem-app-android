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

private var _ic_arrow_swap_horizontal_12: ImageVector? = null

val Icons.ic_arrow_swap_horizontal_12: ImageVector
    get() {
        if (_ic_arrow_swap_horizontal_12 != null) return _ic_arrow_swap_horizontal_12!!
        _ic_arrow_swap_horizontal_12 = ImageVector.Builder(
            name = "ic_arrow_swap_horizontal_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.5 5.5C10.7761 5.5 11 5.72386 11 6C10.9999 8.18874 9.37753 9.75 7.25 9.75H2.61035C2.70332 9.82714 2.78927 9.90003 2.8623 9.95703C2.91927 10.0015 2.98043 10.0639 3.0459 10.0967C3.2682 10.2604 3.31608 10.5745 3.15234 10.7969C3.00907 10.991 2.7519 11.0514 2.54102 10.9541L2.45312 10.9023L2.39453 10.8584C2.28672 10.777 2.04429 10.5908 1.79688 10.376C1.63446 10.235 1.45893 10.0719 1.32031 9.91504C1.25161 9.83725 1.18177 9.74877 1.12598 9.65625C1.07837 9.57728 1.00003 9.43037 1 9.25C1.00001 8.78513 1.48359 8.39606 1.79688 8.12402C2.04428 7.90921 2.28669 7.72306 2.39453 7.6416L2.45312 7.59765C2.67535 7.43398 2.98852 7.48108 3.15234 7.70312C3.31603 7.92547 3.26822 8.23958 3.0459 8.40332C2.98043 8.43605 2.91927 8.49851 2.8623 8.54297C2.78925 8.59998 2.70334 8.67284 2.61035 8.75H7.25C8.83566 8.75 9.99994 7.6261 10 6C10 5.72386 10.2239 5.5 10.5 5.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.84766 1.20312C9.01149 0.981078 9.32465 0.933983 9.54688 1.09765L9.60547 1.1416C9.71331 1.22306 9.95572 1.40921 10.2031 1.62402C10.3655 1.76505 10.5411 1.92809 10.6797 2.08496C10.7484 2.16275 10.8182 2.25123 10.874 2.34375C10.9216 2.42275 11 2.56965 11 2.75C10.9999 3.2149 10.5165 3.6039 10.2031 3.87597C9.95571 4.09078 9.71328 4.27696 9.60547 4.3584L9.54688 4.40234C9.32468 4.56593 9.01149 4.51882 8.84766 4.29687C8.68392 4.07454 8.7318 3.76044 8.9541 3.59668C9.01957 3.56394 9.08073 3.50148 9.1377 3.45703C9.21073 3.40003 9.29668 3.32714 9.38965 3.25H4.75C3.1643 3.25 2 4.37383 2 6C1.99993 6.27608 1.7761 6.5 1.5 6.5C1.2239 6.5 1.00007 6.27608 1 6C1 3.8112 2.62242 2.25 4.75 2.25H9.38965C9.29666 2.17284 9.21075 2.09998 9.1377 2.04297C9.08073 1.99851 9.01957 1.93605 8.9541 1.90332C8.73178 1.73958 8.68397 1.42547 8.84766 1.20312Z"),
            )
        }.build()
        return _ic_arrow_swap_horizontal_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowSwapHorizontal12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_swap_horizontal_12,
        contentDescription = null,
    )
}