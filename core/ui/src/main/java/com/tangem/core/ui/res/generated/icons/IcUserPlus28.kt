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

private var _ic_user_plus_28: ImageVector? = null

val Icons.ic_user_plus_28: ImageVector
    get() {
        if (_ic_user_plus_28 != null) return _ic_user_plus_28!!
        _ic_user_plus_28 = ImageVector.Builder(
            name = "ic_user_plus_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.4395 15.0093C21.1297 15.0094 21.6895 15.569 21.6895 16.2593V18.3198H23.751C24.4411 18.32 25.0007 18.8798 25.001 19.5698C25.001 20.2601 24.4412 20.8197 23.751 20.8198H21.6895V22.8823C21.6893 23.5725 21.1296 24.1322 20.4395 24.1323C19.7492 24.1323 19.1896 23.5726 19.1895 22.8823V20.8198H17.127C16.4366 20.8198 15.877 20.2602 15.877 19.5698C15.8772 18.8797 16.4368 18.3199 17.127 18.3198H19.1895V16.2593C19.1895 15.5689 19.7491 15.0093 20.4395 15.0093Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.96 16.1128C14.6502 16.1128 15.2097 16.6727 15.21 17.3628C15.21 18.0531 14.6503 18.6128 13.96 18.6128H8.66504C6.91675 18.6129 5.49938 20.0303 5.49902 21.7778V22.8813C5.49902 23.5717 4.93938 24.1313 4.24902 24.1313C3.55878 24.1312 2.99902 23.5716 2.99902 22.8813V21.7778C2.99938 18.6492 5.53637 16.1129 8.66504 16.1128H13.96Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2.86768C17.1289 2.86776 19.6659 5.40387 19.666 8.53271C19.666 11.6616 17.1289 14.1977 14 14.1978C10.8711 14.1977 8.33402 11.6616 8.33398 8.53271C8.33409 5.40387 10.8711 2.86776 14 2.86768ZM14 5.36768C12.2515 5.36776 10.8341 6.78492 10.834 8.53271C10.834 10.2806 12.2515 11.6977 14 11.6978C15.7485 11.6977 17.166 10.2806 17.166 8.53271C17.1659 6.78492 15.7485 5.36776 14 5.36768Z"),
            )
        }.build()
        return _ic_user_plus_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUserPlus28Preview() {
    Icon(
        imageVector = Icons.ic_user_plus_28,
        contentDescription = null,
    )
}