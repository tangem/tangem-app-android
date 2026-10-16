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

private var _ic_hanger_20: ImageVector? = null

val Icons.ic_hanger_20: ImageVector
    get() {
        if (_ic_hanger_20 != null) return _ic_hanger_20!!
        _ic_hanger_20 = ImageVector.Builder(
            name = "ic_hanger_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 3C11.1815 3 12.1386 3.9572 12.1387 5.13867C12.1387 6.08841 11.5116 6.58538 11.1211 6.84863L11.1201 6.84961C10.9665 6.95282 10.8768 7.03206 10.8223 7.10547C10.8056 7.12788 10.7911 7.15151 10.7793 7.17871C11.3385 7.23566 11.8931 7.35346 12.4326 7.5332L15.3828 8.5166L15.5596 8.58398C16.4274 8.95183 17 9.80543 17 10.7598C16.9997 11.9697 16.0902 12.9653 14.917 13.1045V14.8271C14.9168 16.0265 13.9435 16.9997 12.7441 17H7.25684C6.05733 17 5.08413 16.0266 5.08398 14.8271V13.1123C3.91363 12.9912 3.00034 12.003 3 10.8008V10.7666C3 9.74391 3.64897 8.83885 4.61621 8.5166L7.56641 7.5332C8.11759 7.34957 8.68441 7.23027 9.25586 7.1748C9.32266 6.31868 9.90768 5.85748 10.2842 5.60449C10.4336 5.5037 10.5204 5.42734 10.5723 5.35742C10.6116 5.30429 10.6387 5.24468 10.6387 5.13867C10.6386 4.78562 10.3531 4.5 10 4.5C9.64703 4.50012 9.36145 4.7857 9.36133 5.13867C9.36133 5.55289 9.02554 5.88867 8.61133 5.88867C8.19711 5.88867 7.86133 5.55289 7.86133 5.13867C7.86145 3.95727 8.8186 3.00012 10 3ZM6.58398 14.8271C6.58414 15.1982 6.88576 15.5 7.25684 15.5H12.7441C13.115 15.4997 13.4168 15.1981 13.417 14.8271V12.1387H6.58398V14.8271ZM11.959 8.95605C10.6874 8.53239 9.31267 8.53251 8.04102 8.95605L5.09082 9.93945C4.74027 10.0562 4.5 10.3853 4.5 10.7666V10.8008C4.50032 11.2026 4.78842 11.5359 5.16895 11.6094C5.37085 11.0438 5.91105 10.6387 6.5459 10.6387H13.4561C14.0874 10.6389 14.6232 11.0406 14.8271 11.6016C15.2119 11.5145 15.4997 11.1714 15.5 10.7598C15.5 10.3875 15.2622 10.0569 14.9092 9.93945L11.959 8.95605Z"),
            )
        }.build()
        return _ic_hanger_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHanger20Preview() {
    Icon(
        imageVector = Icons.ic_hanger_20,
        contentDescription = null,
    )
}