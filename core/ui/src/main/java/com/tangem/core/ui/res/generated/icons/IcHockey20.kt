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

private var _ic_hockey_20: ImageVector? = null

val Icons.ic_hockey_20: ImageVector
    get() {
        if (_ic_hockey_20 != null) return _ic_hockey_20!!
        _ic_hockey_20 = ImageVector.Builder(
            name = "ic_hockey_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.86456 2.32473C11.2746 1.85492 12.7951 1.81441 13.7952 2.74954C13.8179 2.7675 13.8397 2.78717 13.8607 2.80813C13.864 2.81145 13.8671 2.81554 13.8704 2.81887L17.1907 6.13919C18.1889 7.13762 18.1541 8.695 17.6741 10.1353C17.1782 11.6233 16.1366 13.248 14.6927 14.6919C13.2488 16.1358 11.624 17.1774 10.136 17.6734C8.69578 18.1533 7.13837 18.1881 6.13996 17.19L2.80988 13.8599C1.81161 12.8615 1.84561 11.3041 2.3255 9.8638C2.82139 8.37571 3.86296 6.75017 5.30695 5.30618C6.75093 3.86219 8.37648 2.82062 9.86456 2.32473ZM14.6155 5.68509C14.561 6.05822 14.468 6.43507 14.345 6.80423C13.8492 8.29233 12.8076 9.91784 11.3636 11.3618C9.91961 12.8058 8.29403 13.8474 6.80597 14.3433C6.43619 14.4665 6.05862 14.5593 5.68488 14.6138L7.2005 16.1294C7.5808 16.5094 8.3903 16.6741 9.66144 16.2505C10.8853 15.8427 12.3172 14.9463 13.6321 13.6314C14.9471 12.3164 15.8435 10.8845 16.2513 9.66067C16.6748 8.38956 16.5102 7.58005 16.1302 7.19973L14.6155 5.68509ZM12.8001 3.86966C12.4198 3.48951 11.6096 3.32392 10.3382 3.74759C9.11443 4.15544 7.68236 5.05186 6.36749 6.36673C5.05263 7.68159 4.15621 9.11366 3.74835 10.3374C3.32668 11.6028 3.48921 12.411 3.86554 12.7935C3.86718 12.7951 3.86879 12.7967 3.87042 12.7984C4.25059 13.1785 5.06018 13.3429 6.33136 12.9195C7.55515 12.5117 8.98715 11.6152 10.3021 10.3003C11.617 8.98537 12.5134 7.55344 12.9212 6.32962C13.3411 5.06934 13.183 4.26371 12.8099 3.87942L12.8001 3.86966Z"),
            )
        }.build()
        return _ic_hockey_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHockey20Preview() {
    Icon(
        imageVector = Icons.ic_hockey_20,
        contentDescription = null,
    )
}