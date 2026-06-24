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
                pathData = addPathNodes("M12.164 12.1622C13.2512 11.0751 14.9998 11.0586 16.1103 12.1075L16.1142 12.1105L16.1708 12.1622L16.3661 12.3771C17.2738 13.49 17.2082 15.1318 16.1708 16.1691C15.0642 17.2753 13.2705 17.2754 12.164 16.1691C11.0577 15.0626 11.0576 13.2687 12.164 12.1622ZM15.0097 13.131C14.4859 12.7039 13.7128 12.7346 13.2245 13.2228C12.704 13.7434 12.7041 14.5878 13.2245 15.1085C13.7453 15.6291 14.5895 15.629 15.1103 15.1085C15.5984 14.6204 15.6291 13.8481 15.2021 13.3243L15.1103 13.2228L15.0097 13.131Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.6796 4.25989C14.9725 3.96734 15.4474 3.96712 15.7402 4.25989C16.0328 4.5527 16.0327 5.02758 15.7402 5.32044L5.3222 15.7374C5.02935 16.0303 4.55455 16.0302 4.26166 15.7374C3.96879 15.4445 3.96875 14.9698 4.26166 14.6769L14.6796 4.25989Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.83002 3.82825C4.91726 2.74143 6.66596 2.72458 7.7763 3.77356L7.78021 3.77649L7.83685 3.82825L8.03216 4.04309C8.93972 5.15588 8.87397 6.79775 7.83685 7.83509C6.73027 8.94143 4.93656 8.94149 3.83002 7.83509C2.72358 6.72857 2.7235 4.93472 3.83002 3.82825ZM6.67572 4.797C6.15202 4.36988 5.37884 4.40087 4.89056 4.8888C4.36985 5.40947 4.36996 6.2538 4.89056 6.77454C5.41132 7.29516 6.25551 7.29509 6.7763 6.77454C7.26417 6.28641 7.29503 5.51397 6.8681 4.99036L6.7763 4.8888L6.67572 4.797Z"),
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