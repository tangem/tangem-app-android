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

private var _ic_user_24: ImageVector? = null

val Icons.ic_user_24: ImageVector
    get() {
        if (_ic_user_24 != null) return _ic_user_24!!
        _ic_user_24 = ImageVector.Builder(
            name = "ic_user_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.5498 14.0995C17.6169 14.0995 19.9501 16.5625 19.9502 19.4999C19.95 20.052 19.5024 20.4999 18.9502 20.4999C18.398 20.4999 17.9504 20.052 17.9502 19.4999C17.9501 17.6373 16.4827 16.0995 14.5498 16.0995H9.4502C7.58756 16.0995 6.04988 17.567 6.0498 19.4999C6.04965 20.052 5.60199 20.4999 5.0498 20.4999C4.49762 20.4999 4.04996 20.052 4.0498 19.4999C4.04988 16.4328 6.5128 14.0995 9.4502 14.0995H14.5498Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.36426 3.97157C10.4492 2.00945 13.6494 2.00951 15.7344 3.97157C15.7369 3.97399 15.7397 3.97695 15.7422 3.97938C15.7469 3.9839 15.7522 3.98842 15.7568 3.99305C17.8348 6.07137 17.8661 9.42228 15.7344 11.4286C13.6908 13.3516 10.3526 13.5436 8.32129 11.3856C6.39787 9.342 6.20594 6.00299 8.36426 3.97157ZM14.3555 5.42079C13.0404 4.1909 11.0466 4.19355 9.73438 5.4286C8.4932 6.59729 8.50103 8.65828 9.77734 10.0145C10.946 11.2562 13.0079 11.2481 14.3643 9.97157C15.628 8.78195 15.6635 6.74291 14.3555 5.42079Z"),
            )
        }.build()
        return _ic_user_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser24Preview() {
    Icon(
        imageVector = Icons.ic_user_24,
        contentDescription = null,
    )
}