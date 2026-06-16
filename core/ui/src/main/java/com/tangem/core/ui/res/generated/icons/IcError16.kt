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

private var _ic_error_16: ImageVector? = null

val Icons.ic_error_16: ImageVector
    get() {
        if (_ic_error_16 != null) return _ic_error_16!!
        _ic_error_16 = ImageVector.Builder(
            name = "ic_error_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.0791 10.0029C8.47125 10.0427 8.77901 10.3735 8.7793 10.7783C8.77927 11.2087 8.43035 11.5576 8 11.5576C7.59677 11.5574 7.26459 11.2512 7.22461 10.8584L7.2207 10.7783C7.22033 10.3459 7.57225 9.99916 8 9.99902L8.0791 10.0029Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 4.5C8.34508 4.5 8.62484 4.77996 8.625 5.125V8.37207C8.62473 8.71702 8.34501 8.99707 8 8.99707C7.6553 8.9967 7.37527 8.71679 7.375 8.37207V5.125C7.37516 4.78019 7.65524 4.50037 8 4.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.89453 2C10.377 2.00003 10.8396 2.19223 11.1807 2.5332L13.4678 4.81934C13.8096 5.16044 14.0009 5.62319 14.001 6.10547V9.89453C14.0009 10.377 13.8088 10.8396 13.4678 11.1807L10.832 13.8184C10.7148 13.9356 10.5554 14.001 10.3896 14.001H6.10547C5.62316 14.0009 5.16028 13.8087 4.81934 13.4678L2.5332 11.1816C2.19226 10.8407 2.00013 10.3778 2 9.89551V6.10547C2.00013 5.62313 2.19226 5.16028 2.5332 4.81934L4.81934 2.5332C5.16028 2.19226 5.62313 2.00013 6.10547 2H9.89453ZM6.10547 3.25C5.95503 3.25013 5.80991 3.3102 5.70312 3.41699L3.41699 5.70312C3.3102 5.80992 3.25013 5.95503 3.25 6.10547V9.89551C3.25013 10.0459 3.31021 10.1911 3.41699 10.2979L5.70312 12.584C5.80991 12.6907 5.95505 12.7509 6.10547 12.751H10.1309L12.584 10.2969C12.6908 10.19 12.7509 10.0451 12.751 9.89453V6.10547C12.7509 5.95517 12.6908 5.81069 12.584 5.7041L10.2969 3.41699C10.19 3.31017 10.0451 3.25003 9.89453 3.25H6.10547Z"),
            )
        }.build()
        return _ic_error_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcError16Preview() {
    Icon(
        imageVector = Icons.ic_error_16,
        contentDescription = null,
    )
}