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

private var _ic_user_plus_16: ImageVector? = null

val Icons.ic_user_plus_16: ImageVector
    get() {
        if (_ic_user_plus_16 != null) return _ic_user_plus_16!!
        _ic_user_plus_16 = ImageVector.Builder(
            name = "ic_user_plus_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.9375 9.28711C8.2825 9.28731 8.5625 9.56706 8.5625 9.91211C8.56227 10.257 8.28236 10.5369 7.9375 10.5371H5.31348C4.44957 10.5373 3.75 11.2368 3.75 12.0977V12.6436C3.74977 12.9885 3.47003 13.2686 3.125 13.2686C2.78015 13.2683 2.50023 12.9884 2.5 12.6436V12.0977C2.5 10.5449 3.76076 9.28733 5.31348 9.28711H7.9375Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.2334 8.74023C11.5786 8.74023 11.8584 9.02006 11.8584 9.36523V10.3789H12.875C13.2202 10.3789 13.5 10.6587 13.5 11.0039C13.5 11.3491 13.2202 11.6289 12.875 11.6289H11.8584V12.6436C11.858 12.9884 11.5783 13.2686 11.2334 13.2686C10.8886 13.2683 10.6088 12.9883 10.6084 12.6436V11.6289H9.59277C9.24768 11.6288 8.96778 11.349 8.96777 11.0039C8.96777 10.6588 9.24768 10.379 9.59277 10.3789H10.6084V9.36523C10.6084 9.02019 10.8884 8.74044 11.2334 8.74023Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.78711 2.73145C9.34095 2.73165 10.5996 3.99199 10.5996 5.5459C10.5994 7.09959 9.3408 8.36015 7.78711 8.36035C6.23325 8.36035 4.97388 7.09972 4.97363 5.5459C4.97363 3.99186 6.23309 2.73145 7.78711 2.73145ZM7.78711 3.98145C6.92413 3.98145 6.22363 4.68154 6.22363 5.5459C6.22388 6.41005 6.92428 7.11035 7.78711 7.11035C8.64976 7.11015 9.34936 6.40992 9.34961 5.5459C9.34961 4.68166 8.64992 3.98165 7.78711 3.98145Z"),
            )
        }.build()
        return _ic_user_plus_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUserPlus16Preview() {
    Icon(
        imageVector = Icons.ic_user_plus_16,
        contentDescription = null,
    )
}