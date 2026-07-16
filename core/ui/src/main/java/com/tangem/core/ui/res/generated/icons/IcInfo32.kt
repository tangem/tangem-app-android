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

private var _ic_info_32: ImageVector? = null

val Icons.ic_info_32: ImageVector
    get() {
        if (_ic_info_32 != null) return _ic_info_32!!
        _ic_info_32 = ImageVector.Builder(
            name = "ic_info_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 14.75C16.6902 14.7501 17.25 15.3097 17.25 16V22.667C17.2498 23.3571 16.6901 23.9169 16 23.917C15.3099 23.9168 14.7502 23.3571 14.75 22.667V17.25H14.667C13.9766 17.25 13.417 16.6904 13.417 16C13.417 15.3096 13.9766 14.75 14.667 14.75H16Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.793 9.42285L15.7939 9.42383C16.6065 9.48921 17.2498 10.1662 17.25 11C17.2498 11.8741 16.5411 12.5828 15.667 12.583C14.8472 12.583 14.1727 11.9597 14.0918 11.1611L14.083 11C14.0816 10.1654 14.7257 9.49004 15.5352 9.42383C15.5779 9.41941 15.6212 9.41603 15.665 9.41602L15.793 9.42285Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 2.75C23.3184 2.75 29.25 8.68164 29.25 16C29.25 23.3184 23.3184 29.25 16 29.25C8.68164 29.25 2.75 23.3184 2.75 16C2.75 8.68164 8.68164 2.75 16 2.75ZM16 5.25C10.0624 5.25 5.25 10.0624 5.25 16C5.25 21.9376 10.0624 26.75 16 26.75C21.9376 26.75 26.75 21.9376 26.75 16C26.75 10.0624 21.9376 5.25 16 5.25Z"),
            )
        }.build()
        return _ic_info_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcInfo32Preview() {
    Icon(
        imageVector = Icons.ic_info_32,
        contentDescription = null,
    )
}