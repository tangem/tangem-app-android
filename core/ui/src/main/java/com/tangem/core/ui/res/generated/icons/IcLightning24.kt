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

private var _ic_lightning_24: ImageVector? = null

val Icons.ic_lightning_24: ImageVector
    get() {
        if (_ic_lightning_24 != null) return _ic_lightning_24!!
        _ic_lightning_24 = ImageVector.Builder(
            name = "ic_lightning_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.3595 3.78025C10.9266 3.0262 11.8345 2.8655 12.5421 3.06931C13.2525 3.27418 13.958 3.9136 13.9581 4.90037V9.18259H17.9864C18.7453 9.18271 19.3935 9.58356 19.7306 10.1767C20.0723 10.7786 20.0811 11.5684 19.5987 12.2119L13.5821 20.2265C13.0151 20.981 12.1073 21.1412 11.3995 20.9375C10.6889 20.7328 9.98358 20.0944 9.98348 19.1074V14.8252H5.95418C5.1953 14.8251 4.54741 14.4247 4.21004 13.831C3.86775 13.2285 3.85958 12.4383 4.34286 11.7949L10.3585 3.78123L10.3595 3.78025ZM6.07039 12.8252H10.9835C11.5354 12.8255 11.9834 13.2732 11.9835 13.8252V19.0185H11.9864L17.8712 11.1826H12.9581C12.406 11.1826 11.9584 10.7346 11.9581 10.1826V4.98728C11.9564 4.98732 11.9545 4.9872 11.9532 4.98728L6.07039 12.8252Z"),
            )
        }.build()
        return _ic_lightning_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLightning24Preview() {
    Icon(
        imageVector = Icons.ic_lightning_24,
        contentDescription = null,
    )
}