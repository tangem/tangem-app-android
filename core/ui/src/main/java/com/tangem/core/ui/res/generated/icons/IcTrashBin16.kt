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

private var _ic_trash_bin_16: ImageVector? = null

val Icons.ic_trash_bin_16: ImageVector
    get() {
        if (_ic_trash_bin_16 != null) return _ic_trash_bin_16!!
        _ic_trash_bin_16 = ImageVector.Builder(
            name = "ic_trash_bin_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.34277 1.5C10.2274 1.50021 10.9746 2.20188 10.9746 3.10449V3.78516H13.374C13.7189 3.78539 13.9989 4.06525 13.999 4.41016C13.9988 4.75499 13.7189 5.03492 13.374 5.03516H12.9912V12.5713C12.9911 13.6541 12.0925 14.5017 11.0225 14.502H4.97656C3.90634 14.502 3.00795 13.6542 3.00781 12.5713V5.03516H2.625C2.27997 5.03516 2.00024 4.75513 2 4.41016C2.00015 4.06511 2.27992 3.78516 2.625 3.78516H5.02246V3.10449C5.02246 2.20182 5.77059 1.50013 6.65527 1.5H9.34277ZM4.25781 12.5713C4.25795 12.9305 4.56286 13.252 4.97656 13.252H11.0225C11.4359 13.2517 11.7411 12.9304 11.7412 12.5713V5.03516H4.25781V12.5713ZM6.65527 2.75C6.42712 2.75013 6.27246 2.92553 6.27246 3.10449V3.78516H9.72461V3.10449C9.72461 2.92557 9.57084 2.7502 9.34277 2.75H6.65527Z"),
            )
        }.build()
        return _ic_trash_bin_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTrashBin16Preview() {
    Icon(
        imageVector = Icons.ic_trash_bin_16,
        contentDescription = null,
    )
}