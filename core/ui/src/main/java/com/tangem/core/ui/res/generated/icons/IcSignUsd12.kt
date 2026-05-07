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

private var _ic_sign_usd_12: ImageVector? = null

val Icons.ic_sign_usd_12: ImageVector
    get() {
        if (_ic_sign_usd_12 != null) return _ic_sign_usd_12!!
        _ic_sign_usd_12 = ImageVector.Builder(
            name = "ic_sign_usd_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.24954 0.5C6.52568 0.5 6.74953 0.72387 6.74954 1V1.55176C7.42689 1.64637 8.0446 1.86753 8.53958 2.1875C9.20731 2.61916 9.70754 3.27502 9.70755 4.07422C9.70747 4.35029 9.48364 4.57422 9.20755 4.57422C8.93161 4.57404 8.70763 4.35019 8.70755 4.07422C8.70754 3.72796 8.49005 3.34633 7.99661 3.02734C7.5082 2.71164 6.80353 2.5 5.99954 2.5C5.19556 2.5 4.49088 2.71164 4.00247 3.02734C3.50903 3.34633 3.29155 3.72796 3.29153 4.07422C3.29155 4.29659 3.33349 4.46597 3.40482 4.59863C3.47494 4.72893 3.58855 4.8519 3.77493 4.96191C4.16757 5.19353 4.8587 5.35156 5.99954 5.35156C7.18836 5.35156 8.1724 5.49931 8.87259 5.89941C9.23277 6.10525 9.52233 6.38077 9.71829 6.73535C9.91304 7.08786 9.99952 7.48949 9.99954 7.92578C9.99954 8.88789 9.47075 9.5592 8.70755 9.96094C8.15754 10.2504 7.47756 10.4084 6.74954 10.4697V11C6.74954 11.2761 6.52568 11.5 6.24954 11.5C5.97361 11.4998 5.74954 11.276 5.74954 11V10.4922C4.80503 10.4557 3.93479 10.2162 3.27005 9.82227C2.55796 9.40028 1.99954 8.74624 1.99954 7.92578C1.99964 7.64972 2.22346 7.42578 2.49954 7.42578C2.77563 7.42578 2.99945 7.64972 2.99954 7.92578C2.99954 8.25079 3.22518 8.63323 3.77982 8.96191C4.32353 9.28411 5.107 9.5 5.99954 9.5C6.92806 9.5 7.71052 9.35574 8.24173 9.07617C8.74508 8.81124 8.99954 8.44501 8.99954 7.92578C8.99952 7.62162 8.94005 7.39388 8.84329 7.21875C8.74761 7.04562 8.59956 6.89505 8.3765 6.76758C7.91001 6.50102 7.14403 6.35156 5.99954 6.35156C4.80735 6.35156 3.89439 6.19405 3.26614 5.82324C2.94235 5.63213 2.69081 5.38245 2.52396 5.07227C2.35836 4.76428 2.29155 4.42445 2.29153 4.07422C2.29155 3.27502 2.79178 2.61916 3.4595 2.1875C4.07394 1.79032 4.87755 1.54733 5.74954 1.50781V1C5.74956 0.724025 5.97362 0.50025 6.24954 0.5Z"),
            )
        }.build()
        return _ic_sign_usd_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignUsd12Preview() {
    Icon(
        imageVector = Icons.ic_sign_usd_12,
        contentDescription = null,
    )
}