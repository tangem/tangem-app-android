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

private var _ic_logo_tangem_32: ImageVector? = null

val Icons.ic_logo_tangem_32: ImageVector
    get() {
        if (_ic_logo_tangem_32 != null) return _ic_logo_tangem_32!!
        _ic_logo_tangem_32 = ImageVector.Builder(
            name = "ic_logo_tangem_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.6657 15.5396V27.5H10.7405C9.0809 27.5 8.25152 27.4996 7.61691 27.2024C7.05949 26.9423 6.6082 26.5248 6.32366 26.012C6.00058 25.4281 6 24.6652 6 23.1384V15.5396H12.6657Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M26 23.1384C26 24.6652 25.9994 25.4281 25.6763 26.012C25.3918 26.5248 24.9405 26.9423 24.3831 27.2024C23.7485 27.4996 22.9191 27.5 21.2595 27.5H19.3343V16.3889L19.3329 15.5396H26V23.1384Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.2595 4.5C22.9191 4.5 23.7485 4.50038 24.3831 4.79761C24.9405 5.05767 25.3918 5.47518 25.6763 5.98804C25.9994 6.57188 26 7.33479 26 8.86163V10.4802H6V8.86163C6 7.33479 6.00058 6.57188 6.32366 5.98804C6.60628 5.47518 7.05949 5.05944 7.61691 4.79761C8.25152 4.50038 9.0809 4.5 10.7405 4.5H21.2595Z"),
            )
        }.build()
        return _ic_logo_tangem_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLogoTangem32Preview() {
    Icon(
        imageVector = Icons.ic_logo_tangem_32,
        contentDescription = null,
    )
}