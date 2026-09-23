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

private var _ic_percent_20: ImageVector? = null

val Icons.ic_percent_20: ImageVector
    get() {
        if (_ic_percent_20 != null) return _ic_percent_20!!
        _ic_percent_20 = ImageVector.Builder(
            name = "ic_percent_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.164 12.162C13.2512 11.0749 14.9998 11.0583 16.1103 12.1073L16.1142 12.1102L16.1708 12.162L16.3661 12.3768C17.2738 13.4897 17.2082 15.1315 16.1708 16.1688C15.0642 17.275 13.2705 17.2752 12.164 16.1688C11.0577 15.0623 11.0576 13.2684 12.164 12.162ZM15.0097 13.1307C14.4859 12.7036 13.7128 12.7344 13.2245 13.2225C12.704 13.7432 12.7041 14.5876 13.2245 15.1083C13.7453 15.6289 14.5895 15.6287 15.1103 15.1083C15.5984 14.6202 15.6291 13.8478 15.2021 13.3241L15.1103 13.2225L15.0097 13.1307Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.6796 4.25965C14.9725 3.9671 15.4474 3.96688 15.7402 4.25965C16.0328 4.55245 16.0327 5.02734 15.7402 5.32019L5.3222 15.7372C5.02935 16.03 4.55455 16.0299 4.26166 15.7372C3.96879 15.4443 3.96875 14.9695 4.26166 14.6766L14.6796 4.25965Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.83002 3.82801C4.91726 2.74118 6.66596 2.72434 7.7763 3.77332L7.78021 3.77625L7.83685 3.82801L8.03216 4.04285C8.93972 5.15564 8.87397 6.79751 7.83685 7.83484C6.73027 8.94118 4.93656 8.94125 3.83002 7.83484C2.72358 6.72832 2.7235 4.93448 3.83002 3.82801ZM6.67572 4.79676C6.15202 4.36964 5.37884 4.40062 4.89056 4.88855C4.36985 5.40922 4.36996 6.25356 4.89056 6.7743C5.41132 7.29492 6.25551 7.29485 6.7763 6.7743C7.26417 6.28616 7.29503 5.51372 6.8681 4.99012L6.7763 4.88855L6.67572 4.79676Z"),
            )
        }.build()
        return _ic_percent_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPercent20Preview() {
    Icon(
        imageVector = Icons.ic_percent_20,
        contentDescription = null,
    )
}