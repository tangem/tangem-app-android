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

private var _ic_arrow_cross_horizontal_32: ImageVector? = null

val Icons.ic_arrow_cross_horizontal_32: ImageVector
    get() {
        if (_ic_arrow_cross_horizontal_32 != null) return _ic_arrow_cross_horizontal_32!!
        _ic_arrow_cross_horizontal_32 = ImageVector.Builder(
            name = "ic_arrow_cross_horizontal_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.6436 18.1856C16.3105 17.6941 17.2498 17.836 17.7412 18.5029L18.9404 20.1318C19.4936 20.8826 20.2673 21.2861 21.0664 21.2861H24.4238L24.2637 21.1231C23.6831 20.5321 23.6913 19.5816 24.2822 19.001C24.8732 18.4207 25.8228 18.4297 26.4033 19.0205L29.0703 21.7344C29.6435 22.3179 29.6434 23.2534 29.0703 23.8369L26.4033 26.5508C25.8228 27.1417 24.8732 27.1507 24.2822 26.5703C23.6913 25.9898 23.6832 25.0402 24.2637 24.4492L24.4238 24.2861H21.0664C19.199 24.2861 17.5721 23.3317 16.5254 21.9111L15.3262 20.2822C14.8348 19.6154 14.9768 18.677 15.6436 18.1856ZM24.2822 5.4297C24.8732 4.84933 25.8228 4.85835 26.4033 5.44923L29.0703 8.1631C29.6434 8.74664 29.6435 9.68212 29.0703 10.2656L26.4033 12.9795C25.8228 13.5704 24.8732 13.5793 24.2822 12.999C23.6913 12.4185 23.6831 11.4679 24.2637 10.877L24.4238 10.7139H21.0664C20.2673 10.714 19.4936 11.1174 18.9404 11.8682L18.9375 11.874L11.4736 21.9102L11.4746 21.9111C10.4279 23.3317 8.80097 24.2861 6.93359 24.2861H4C3.17172 24.2861 2.50024 23.6144 2.5 22.7861C2.5 21.9577 3.17157 21.2861 4 21.2861H6.93359C7.73268 21.2861 8.50639 20.8826 9.05957 20.1318L9.0625 20.126L16.5293 10.084C17.5761 8.6661 19.2011 7.71397 21.0664 7.71388H24.4238L24.2637 7.55079C23.6832 6.95984 23.6913 6.01026 24.2822 5.4297ZM6.93359 7.71388C8.80097 7.71397 10.4279 8.66834 11.4746 10.0889L12.6738 11.7178C13.1652 12.3847 13.0232 13.323 12.3564 13.8145C11.6895 14.3059 10.7502 14.164 10.2588 13.4971L9.05957 11.8682C8.50639 11.1174 7.73268 10.714 6.93359 10.7139H4C3.17157 10.7139 2.5 10.0423 2.5 9.21388C2.50022 8.38564 3.17171 7.71388 4 7.71388H6.93359Z"),
            )
        }.build()
        return _ic_arrow_cross_horizontal_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowCrossHorizontal32Preview() {
    Icon(
        imageVector = Icons.ic_arrow_cross_horizontal_32,
        contentDescription = null,
    )
}