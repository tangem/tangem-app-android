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

private var _ic_lightning_20: ImageVector? = null

val Icons.ic_lightning_20: ImageVector
    get() {
        if (_ic_lightning_20 != null) return _ic_lightning_20!!
        _ic_lightning_20 = ImageVector.Builder(
            name = "ic_lightning_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.78324 3.60144C9.20285 3.0186 9.89024 2.88265 10.4366 3.04675C10.9849 3.21181 11.4892 3.71215 11.4893 4.44519V7.82507H14.5059C15.6587 7.8254 16.4492 9.11889 15.7227 10.1307L11.2168 16.3983C10.7972 16.9811 10.1098 17.1171 9.56352 16.953C9.01523 16.7879 8.51089 16.2875 8.51078 15.5546V12.1737H5.49418C4.34159 12.1733 3.54885 10.8807 4.27738 9.86804L8.78324 3.60144ZM5.54594 10.6737H9.26078C9.67464 10.674 10.0106 11.0098 10.0108 11.4237V15.5057L14.4541 9.32507H10.7393C10.3253 9.3248 9.98934 8.98908 9.9893 8.57507V4.49207L5.54594 10.6737Z"),
            )
        }.build()
        return _ic_lightning_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLightning20Preview() {
    Icon(
        imageVector = Icons.ic_lightning_20,
        contentDescription = null,
    )
}