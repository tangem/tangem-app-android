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

private var _ic_share_ios_16: ImageVector? = null

val Icons.ic_share_ios_16: ImageVector
    get() {
        if (_ic_share_ios_16 != null) return _ic_share_ios_16!!
        _ic_share_ios_16 = ImageVector.Builder(
            name = "ic_share_ios_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6 5.66113C6.3312 5.6613 6.59959 5.92951 6.59961 6.26074C6.59936 6.59179 6.33106 6.86019 6 6.86035H5.58301C5.17837 6.86065 4.84977 7.18908 4.84961 7.59375V11.5674C4.84989 11.972 5.17844 12.3005 5.58301 12.3008H10.417C10.8215 12.3005 11.1501 11.9719 11.1504 11.5674V7.59375C11.1502 7.18909 10.8216 6.86066 10.417 6.86035H10C9.66892 6.8602 9.40063 6.5918 9.40039 6.26074C9.40041 5.9295 9.66879 5.66128 10 5.66113H10.417C11.4843 5.66144 12.3494 6.52638 12.3496 7.59375V11.5674C12.3493 12.6347 11.4843 13.5007 10.417 13.501H5.58301C4.51573 13.5007 3.65067 12.6347 3.65039 11.5674V7.59375C3.65055 6.52637 4.51566 5.66143 5.58301 5.66113H6Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 2C8.15901 2.0001 8.3114 2.06333 8.42383 2.17578L9.89746 3.64941C10.1315 3.88377 10.1317 4.26386 9.89746 4.49805C9.66309 4.73194 9.28297 4.73128 9.04883 4.49707L8.59961 4.04785V8.78027C8.59926 9.11133 8.33113 9.37987 8 9.37988C7.66886 9.37988 7.40074 9.11133 7.40039 8.78027V4.0459L6.9502 4.49707C6.71603 4.73094 6.3368 4.7318 6.10254 4.49805C5.86846 4.26401 5.868 3.8838 6.10156 3.64941L7.5752 2.17578C7.68763 2.06331 7.84098 2.0001 8 2Z"),
            )
        }.build()
        return _ic_share_ios_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShareIos16Preview() {
    Icon(
        imageVector = Icons.ic_share_ios_16,
        contentDescription = null,
    )
}