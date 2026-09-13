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

private var _ic_banknote_arrow_down_16: ImageVector? = null

val Icons.ic_banknote_arrow_down_16: ImageVector
    get() {
        if (_ic_banknote_arrow_down_16 != null) return _ic_banknote_arrow_down_16!!
        _ic_banknote_arrow_down_16 = ImageVector.Builder(
            name = "ic_banknote_arrow_down_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.6016 4.30664C11.5515 4.30691 12.3114 5.08316 12.3115 6.02734V7.13672C12.3114 7.48162 12.0314 7.76146 11.6865 7.76172C11.3414 7.76172 11.0617 7.48178 11.0615 7.13672V6.02734C11.0614 5.76155 10.8493 5.55691 10.6016 5.55664H4.96094C4.71301 5.55664 4.50108 5.76137 4.50098 6.02734V9.97266C4.50111 10.2386 4.71302 10.4434 4.96094 10.4434H9.34863C9.69333 10.4438 9.97344 10.7236 9.97363 11.0684C9.9735 11.4132 9.69337 11.6929 9.34863 11.6934H4.96094C4.01074 11.6934 3.25013 10.917 3.25 9.97266V6.02734C3.25011 5.083 4.01072 4.30664 4.96094 4.30664H10.6016Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.6865 8.29883C12.0315 8.29909 12.3115 8.57881 12.3115 8.92383V9.5918C12.5507 9.45238 12.8623 9.48376 13.0674 9.68848C13.311 9.93259 13.3113 10.3293 13.0674 10.5732L12.1289 11.5107C12.0488 11.5909 11.9507 11.6405 11.8486 11.668C11.8349 11.6717 11.8217 11.6779 11.8076 11.6807C11.7276 11.6964 11.6454 11.6964 11.5654 11.6807C11.5456 11.6768 11.5269 11.6698 11.5078 11.6641C11.4869 11.6578 11.4656 11.653 11.4453 11.6445C11.434 11.6398 11.4241 11.6323 11.4131 11.627C11.3529 11.5975 11.2952 11.5607 11.2451 11.5107L10.3066 10.5732C10.0626 10.3292 10.0626 9.93255 10.3066 9.68848C10.5114 9.48368 10.8222 9.45193 11.0615 9.59082V8.92383C11.0615 8.57865 11.3413 8.29883 11.6865 8.29883Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.64844 6.86719C7.27432 6.24182 8.28925 6.24124 8.91504 6.86719C8.9181 6.87026 8.9199 6.87478 8.92285 6.87793C9.53893 7.50423 9.53699 8.51041 8.91504 9.13281C8.32853 9.71874 7.39985 9.75525 6.77051 9.24316L6.64844 9.13281C6.02259 8.50721 6.02309 7.49283 6.64844 6.86719ZM7.97559 7.70508C7.83906 7.61502 7.65204 7.62959 7.53125 7.75C7.39371 7.88764 7.39466 8.1109 7.53125 8.24805C7.66938 8.38595 7.89287 8.38536 8.03027 8.24805C8.15051 8.12742 8.1662 7.94195 8.07617 7.80566L8.03027 7.75098L7.97559 7.70508Z"),
            )
        }.build()
        return _ic_banknote_arrow_down_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBanknoteArrowDown16Preview() {
    Icon(
        imageVector = Icons.ic_banknote_arrow_down_16,
        contentDescription = null,
    )
}