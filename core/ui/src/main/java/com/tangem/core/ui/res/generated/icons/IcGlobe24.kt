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

private var _ic_globe_24: ImageVector? = null

val Icons.ic_globe_24: ImageVector
    get() {
        if (_ic_globe_24 != null) return _ic_globe_24!!
        _ic_globe_24 = ImageVector.Builder(
            name = "ic_globe_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C17.5343 2 22 6.46572 22 12C22 17.5343 17.5343 22 12 22C6.46572 22 2 17.5343 2 12C2 6.46572 6.46572 2 12 2ZM4.06348 13C4.46053 16.206 6.74119 18.8119 9.77051 19.6846C8.44522 17.6612 7.70262 15.3488 7.53809 13H4.06348ZM16.4619 13C16.2974 15.349 15.554 17.6611 14.2285 19.6846C17.2583 18.8122 19.5394 16.2064 19.9365 13H16.4619ZM9.54199 13C9.7275 15.2743 10.5465 17.5016 12 19.3662C13.4535 17.5016 14.2725 15.2743 14.458 13H9.54199ZM9.77051 4.31445C6.74106 5.18703 4.46054 7.79386 4.06348 11H7.53809C7.70264 8.65092 8.44494 6.33795 9.77051 4.31445ZM12 4.63281C10.5462 6.49755 9.72752 8.72546 9.54199 11H14.458C14.2725 8.72546 13.4538 6.49755 12 4.63281ZM14.2285 4.31445C15.5543 6.33806 16.2974 8.65075 16.4619 11H19.9365C19.5394 7.79354 17.2584 5.18677 14.2285 4.31445Z"),
            )
        }.build()
        return _ic_globe_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGlobe24Preview() {
    Icon(
        imageVector = Icons.ic_globe_24,
        contentDescription = null,
    )
}