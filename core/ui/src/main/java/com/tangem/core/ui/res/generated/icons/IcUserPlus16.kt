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

private var _ic_user_plus_16: ImageVector? = null

val Icons.ic_user_plus_16: ImageVector
    get() {
        if (_ic_user_plus_16 != null) return _ic_user_plus_16!!
        _ic_user_plus_16 = ImageVector.Builder(
            name = "ic_user_plus_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.335 8.65723C11.6799 8.65749 11.96 8.93721 11.96 9.28223V10.1953H12.876C13.2208 10.1957 13.501 10.4754 13.501 10.8203C13.5008 11.1651 13.2207 11.4449 12.876 11.4453H11.96V12.3594C11.9596 12.7041 11.6797 12.9841 11.335 12.9844C10.99 12.9844 10.7103 12.7042 10.71 12.3594V11.4453H9.79492C9.44983 11.4453 9.17005 11.1654 9.16992 10.8203C9.16992 10.4751 9.44974 10.1953 9.79492 10.1953H10.71V9.28223C10.71 8.93705 10.9898 8.65723 11.335 8.65723Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.6416 9.16992C8.98678 9.16992 9.2666 9.44974 9.2666 9.79492C9.26634 10.1399 8.98662 10.4199 8.6416 10.4199H6.17871C5.38897 10.42 4.75018 11.0591 4.75 11.8457V12.3584C4.74974 12.7034 4.47002 12.9834 4.125 12.9834C3.77998 12.9834 3.50026 12.7034 3.5 12.3584V11.8457C3.50018 10.3671 4.70019 9.17001 6.17871 9.16992H8.6416Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.4375 3.0166C9.91699 3.01687 11.116 4.2168 11.1162 5.69629C11.1161 7.17581 9.91702 8.37669 8.4375 8.37695C6.95783 8.37687 5.75892 7.17592 5.75879 5.69629C5.75896 4.21669 6.95785 3.01669 8.4375 3.0166ZM8.4375 4.2666C7.64886 4.26669 7.00896 4.90639 7.00879 5.69629C7.00892 6.48622 7.64884 7.12687 8.4375 7.12695C9.22601 7.12669 9.86608 6.48611 9.86621 5.69629C9.86604 4.9065 9.22599 4.26687 8.4375 4.2666Z"),
            )
        }.build()
        return _ic_user_plus_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUserPlus16Preview() {
    Icon(
        imageVector = Icons.ic_user_plus_16,
        contentDescription = null,
    )
}