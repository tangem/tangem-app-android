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

private var _ic_arrow_refresh_32: ImageVector? = null

val Icons.ic_arrow_refresh_32: ImageVector
    get() {
        if (_ic_arrow_refresh_32 != null) return _ic_arrow_refresh_32!!
        _ic_arrow_refresh_32 = ImageVector.Builder(
            name = "ic_arrow_refresh_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M26.751 14.75C27.4413 14.75 28.001 15.3096 28.001 16C28.001 22.628 22.6289 27.9999 16.001 28C12.1256 28 8.69027 26.1578 6.5 23.3105V24.959C6.49973 25.6491 5.94019 26.209 5.25 26.209C4.56008 26.2087 4.00027 25.6489 4 24.959V20.4795C4 19.7893 4.55991 19.2298 5.25 19.2295H9.72949C10.4198 19.2295 10.9795 19.7891 10.9795 20.4795C10.9794 21.1697 10.4198 21.7295 9.72949 21.7295H8.43555C10.1698 24.0207 12.9119 25.5 16.001 25.5C21.2482 25.4999 25.501 21.2473 25.501 16C25.501 15.3096 26.0606 14.75 26.751 14.75Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.001 4C19.8754 4.00001 23.3106 5.84055 25.501 8.68652V7.04102C25.5012 6.35088 26.0608 5.79102 26.751 5.79102C27.4412 5.79102 28.0007 6.35088 28.001 7.04102V11.5205C28.0008 12.2107 27.4412 12.7705 26.751 12.7705H22.2715C21.5813 12.7704 21.0216 12.2106 21.0215 11.5205C21.0217 10.8305 21.5814 10.2707 22.2715 10.2705H23.5664C21.8322 7.97924 19.0901 6.50001 16.001 6.5C10.7538 6.5001 6.50106 10.7528 6.50098 16C6.50088 16.6903 5.94126 17.25 5.25098 17.25C4.56071 17.25 4.00108 16.6902 4.00098 16C4.00106 9.37211 9.3731 4.0001 16.001 4Z"),
            )
        }.build()
        return _ic_arrow_refresh_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRefresh32Preview() {
    Icon(
        imageVector = Icons.ic_arrow_refresh_32,
        contentDescription = null,
    )
}