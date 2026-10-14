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

private var _ic_chair_32: ImageVector? = null

val Icons.ic_chair_32: ImageVector
    get() {
        if (_ic_chair_32 != null) return _ic_chair_32!!
        _ic_chair_32 = ImageVector.Builder(
            name = "ic_chair_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.1114 2.99976C24.0567 2.99989 26.4452 5.38733 26.4453 8.33276V13.4871C27.112 13.7428 27.7068 14.1758 28.1573 14.7527C28.9112 15.7185 29.1781 16.9781 28.8809 18.1667L27.5791 23.3728C27.388 24.137 26.9853 24.8031 26.4453 25.3191V27.4998C26.4453 28.3281 25.7737 28.9996 24.9453 28.9998C24.117 28.9996 23.4453 28.3281 23.4453 27.4998V26.4451H8.55472V27.4998C8.55472 28.3281 7.88303 28.9996 7.05472 28.9998C6.22642 28.9996 5.55472 28.3281 5.55472 27.4998V25.3191C5.01463 24.8031 4.61211 24.1371 4.42093 23.3728L3.11917 18.1667C2.82194 16.978 3.08868 15.7185 3.8428 14.7527C4.29334 14.1757 4.88791 13.7428 5.55472 13.4871V8.33276C5.55489 5.38725 7.94322 2.99976 10.8887 2.99976H21.1114ZM25.0205 16.2224C24.571 16.2224 24.1793 16.5288 24.0703 16.9646L23.2617 20.197C23.0948 20.8646 22.4948 21.3336 21.8067 21.3337H10.1934C9.5052 21.3337 8.90536 20.8646 8.73831 20.197L7.92972 16.9646C7.82076 16.5288 7.429 16.2225 6.97952 16.2224C6.67785 16.2224 6.39265 16.3617 6.20706 16.5994C6.02172 16.837 5.95624 17.1469 6.02933 17.4392L7.33108 22.6453C7.44873 23.1148 7.87109 23.4451 8.3555 23.4451H23.6446C24.1289 23.445 24.5514 23.1148 24.669 22.6453L25.9707 17.4392C26.0438 17.147 25.9782 16.837 25.793 16.5994C25.6075 16.3618 25.3221 16.2225 25.0205 16.2224ZM10.8887 5.99976C9.59979 5.99976 8.55489 7.04439 8.55472 8.33276V13.5476C9.66696 14.0276 10.5316 15.0037 10.8399 16.2361L11.3643 18.3337H20.6358L21.1602 16.2361C21.4685 15.0036 22.3329 14.0276 23.4453 13.5476V8.33276C23.4452 7.04447 22.4002 5.99989 21.1114 5.99976H10.8887Z"),
            )
        }.build()
        return _ic_chair_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChair32Preview() {
    Icon(
        imageVector = Icons.ic_chair_32,
        contentDescription = null,
    )
}