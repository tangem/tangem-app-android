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

private var _ic_arrow_refresh_16: ImageVector? = null

val Icons.ic_arrow_refresh_16: ImageVector
    get() {
        if (_ic_arrow_refresh_16 != null) return _ic_arrow_refresh_16!!
        _ic_arrow_refresh_16 = ImageVector.Builder(
            name = "ic_arrow_refresh_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.8789 7.37695C13.224 7.37704 13.5039 7.65683 13.5039 8.00195C13.5039 11.0407 11.0407 13.5038 8.00195 13.5039C6.28439 13.5039 4.7571 12.713 3.75 11.4814V12.0664C3.74971 12.4113 3.46988 12.6913 3.125 12.6914C2.7801 12.6913 2.50029 12.4113 2.5 12.0664V10.0342C2.5 9.68907 2.77992 9.4093 3.125 9.40918H5.15723C5.50229 9.40932 5.78223 9.68909 5.78223 10.0342C5.78207 10.3791 5.50219 10.659 5.15723 10.6592H4.69141C5.47041 11.6307 6.66264 12.2539 8.00195 12.2539C10.3503 12.2538 12.2539 10.3503 12.2539 8.00195C12.2539 7.65677 12.5337 7.37695 12.8789 7.37695Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.00195 2.5C9.71877 2.50008 11.2466 3.28982 12.2539 4.52051V3.93848C12.2539 3.5933 12.5337 3.31348 12.8789 3.31348C13.224 3.31356 13.5039 3.59335 13.5039 3.93848V5.9707C13.5036 6.31553 13.2238 6.59562 12.8789 6.5957H10.8467C10.5018 6.59558 10.222 6.31551 10.2217 5.9707C10.2217 5.6256 10.5016 5.34582 10.8467 5.3457H11.3135C10.5345 4.37354 9.34185 3.75008 8.00195 3.75C5.65376 3.75025 3.75106 5.65377 3.75098 8.00195C3.75087 8.34682 3.47079 8.6266 3.12598 8.62695C2.78087 8.62695 2.50109 8.34704 2.50098 8.00195C2.50106 4.9634 4.96342 2.50025 8.00195 2.5Z"),
            )
        }.build()
        return _ic_arrow_refresh_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRefresh16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_refresh_16,
        contentDescription = null,
    )
}