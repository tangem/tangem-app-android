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

private var _ic_logo_tangem_16: ImageVector? = null

val Icons.ic_logo_tangem_16: ImageVector
    get() {
        if (_ic_logo_tangem_16 != null) return _ic_logo_tangem_16!!
        _ic_logo_tangem_16 = ImageVector.Builder(
            name = "ic_logo_tangem_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.66602 7.82031V12.5H5.89648C5.23264 12.5 4.90033 12.5001 4.64648 12.3838C4.42356 12.282 4.2427 12.1186 4.12891 11.918C3.99971 11.6896 4 11.3911 4 10.7939V7.82031H6.66602Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 10.7939C12 11.3911 12.0003 11.6896 11.8711 11.918C11.7572 12.1187 11.5756 12.282 11.3525 12.3838C11.0987 12.4999 10.767 12.5 10.1035 12.5H9.33398V8.15234L9.33301 7.82031H12V10.7939Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.1035 3.5C10.7674 3.5 11.0997 3.4999 11.3535 3.61621C11.5764 3.71799 11.7573 3.88137 11.8711 4.08203C12.0003 4.31043 12 4.60892 12 5.20605V5.83984H4V5.20605C4 4.60892 3.99971 4.31043 4.12891 4.08203C4.24193 3.88137 4.42356 3.71868 4.64648 3.61621C4.90033 3.4999 5.23264 3.5 5.89648 3.5H10.1035Z"),
            )
        }.build()
        return _ic_logo_tangem_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLogoTangem16Preview() {
    Icon(
        imageVector = Icons.ic_logo_tangem_16,
        contentDescription = null,
    )
}