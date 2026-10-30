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

private var _ic_hockey_32: ImageVector? = null

val Icons.ic_hockey_32: ImageVector
    get() {
        if (_ic_hockey_32 != null) return _ic_hockey_32!!
        _ic_hockey_32 = ImageVector.Builder(
            name = "ic_hockey_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.694 3.05127C18.0844 2.25487 20.7621 2.15857 22.5221 3.8667C22.5392 3.88231 22.5573 3.89703 22.5738 3.91358C22.5919 3.93167 22.6089 3.95095 22.6266 3.96924L28.0846 9.42725C29.8434 11.1864 29.752 13.893 28.9479 16.3062C28.1119 18.8147 26.3631 21.5384 23.9508 23.9507C21.5385 26.363 18.8149 28.1118 16.3063 28.9478C13.8932 29.7519 11.1865 29.8433 9.42736 28.0845L3.91564 22.5728C2.15664 20.8137 2.24742 18.107 3.05138 15.6939C3.88734 13.1852 5.63706 10.4617 8.04943 8.04932C10.4618 5.63696 13.1853 3.88724 15.694 3.05127ZM23.8092 9.39405C23.7157 9.86503 23.5898 10.3342 23.4371 10.7925C22.6012 13.3011 20.8523 16.0247 18.4401 18.437C16.0278 20.8493 13.3041 22.5981 10.7955 23.4341C10.336 23.5872 9.86543 23.7135 9.39318 23.8071L11.5484 25.9634C12.0714 26.4859 13.283 26.7935 15.358 26.1021C17.3379 25.4423 19.6747 23.9835 21.8287 21.8296C23.9829 19.6754 25.4423 17.338 26.1022 15.3579C26.7936 13.283 26.486 12.0713 25.9635 11.5483L23.8092 9.39405ZM20.4508 6.03662C19.9279 5.51405 18.7171 5.20663 16.6422 5.89795C14.6621 6.55778 12.3248 8.01616 10.1705 10.1704C8.01627 12.3247 6.55789 14.662 5.89806 16.6421C5.21302 18.6981 5.50918 19.9046 6.02306 20.4351C6.02784 20.4397 6.03298 20.444 6.03771 20.4487C6.56045 20.9715 7.77181 21.28 9.84728 20.5884C11.8273 19.9285 14.1638 18.4691 16.318 16.3149C18.4721 14.1608 19.9316 11.8243 20.5914 9.84424C21.2683 7.81267 20.9865 6.61002 20.484 6.06983L20.4508 6.03662Z"),
            )
        }.build()
        return _ic_hockey_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHockey32Preview() {
    Icon(
        imageVector = Icons.ic_hockey_32,
        contentDescription = null,
    )
}