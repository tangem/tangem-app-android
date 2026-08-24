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

private var _ic_filter_28: ImageVector? = null

val Icons.ic_filter_28: ImageVector
    get() {
        if (_ic_filter_28 != null) return _ic_filter_28!!
        _ic_filter_28 = ImageVector.Builder(
            name = "ic_filter_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.8174 14.874C13.1771 14.874 15.1505 16.4994 15.7041 18.6875H23.75C24.4404 18.6875 25 19.2471 25 19.9375C24.9998 20.6277 24.4402 21.1875 23.75 21.1875H15.7041C15.1503 23.3753 13.1768 25 10.8174 25C8.45809 24.9999 6.48544 23.3751 5.93164 21.1875H4.25C3.55976 21.1875 3.00018 20.6277 3 19.9375C3 19.2471 3.55964 18.6875 4.25 18.6875H5.93164C6.48522 16.4995 8.45786 14.8742 10.8174 14.874ZM10.8174 17.374C9.42036 17.3742 8.27637 18.5156 8.27637 19.9375C8.27655 21.3592 9.42047 22.4998 10.8174 22.5C12.2144 22.5 13.3592 21.3593 13.3594 19.9375C13.3594 18.5155 12.2146 17.374 10.8174 17.374ZM16.9111 3C19.2706 3 21.2441 4.62471 21.7979 6.8125H23.75C24.4402 6.81251 24.9998 7.37233 25 8.0625C25 8.75285 24.4403 9.3125 23.75 9.3125H21.7979C21.2443 11.5006 19.2708 13.126 16.9111 13.126C14.5516 13.1258 12.579 11.5005 12.0254 9.3125H4.25C3.55965 9.3125 3 8.75285 3 8.0625C3.00021 7.37232 3.55977 6.8125 4.25 6.8125H12.0254C12.5792 4.62485 14.5518 3.00015 16.9111 3ZM16.9111 5.5C15.5142 5.50018 14.3703 6.64078 14.3701 8.0625C14.3701 9.48437 15.5141 10.6258 16.9111 10.626C18.3083 10.626 19.4531 9.48449 19.4531 8.0625C19.4529 6.64067 18.3082 5.5 16.9111 5.5Z"),
            )
        }.build()
        return _ic_filter_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcFilter28Preview() {
    Icon(
        imageVector = Icons.ic_filter_28,
        contentDescription = null,
    )
}