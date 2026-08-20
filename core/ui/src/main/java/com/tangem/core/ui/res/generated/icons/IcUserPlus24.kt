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

private var _ic_user_plus_24: ImageVector? = null

val Icons.ic_user_plus_24: ImageVector
    get() {
        if (_ic_user_plus_24 != null) return _ic_user_plus_24!!
        _ic_user_plus_24 = ImageVector.Builder(
            name = "ic_user_plus_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7598 13.7598C13.3119 13.76 13.7598 14.2076 13.7598 14.7598C13.7596 15.3118 13.3118 15.7595 12.7598 15.7598H8.25586C6.73336 15.7598 5.50013 16.9929 5.5 18.5127V19.4512C5.49987 20.0033 5.0522 20.4512 4.5 20.4512C3.9478 20.4512 3.50013 20.0033 3.5 19.4512V18.5127C3.50013 15.8871 5.63007 13.7598 8.25586 13.7598H12.7598Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.6836 12.8213C18.2357 12.8216 18.6836 13.2692 18.6836 13.8213V15.6367H20.5C21.0522 15.6368 21.5 16.0845 21.5 16.6367C21.4997 17.1887 21.052 17.6366 20.5 17.6367H18.6836V19.4512C18.6835 20.0032 18.2356 20.4509 17.6836 20.4512C17.1313 20.4512 16.6837 20.0034 16.6836 19.4512V17.6367H14.8662C14.3141 17.6367 13.8665 17.1888 13.8662 16.6367C13.8662 16.0844 14.3139 15.6367 14.8662 15.6367H16.6836V13.8213C16.6836 13.269 17.1313 12.8213 17.6836 12.8213Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.0068 2.5C14.63 2.50026 16.7568 4.62681 16.7568 7.25C16.7568 9.87319 14.63 11.9997 12.0068 12C9.38348 12 7.25684 9.87335 7.25684 7.25C7.25684 4.62665 9.38348 2.5 12.0068 2.5ZM12.0068 4.5C10.4881 4.5 9.25684 5.73122 9.25684 7.25C9.25684 8.76878 10.4881 10 12.0068 10C13.5254 9.99974 14.7568 8.76862 14.7568 7.25C14.7568 5.73138 13.5254 4.50026 12.0068 4.5Z"),
            )
        }.build()
        return _ic_user_plus_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUserPlus24Preview() {
    Icon(
        imageVector = Icons.ic_user_plus_24,
        contentDescription = null,
    )
}