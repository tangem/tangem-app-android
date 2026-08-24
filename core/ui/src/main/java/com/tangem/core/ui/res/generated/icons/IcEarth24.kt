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

private var _ic_earth_24: ImageVector? = null

val Icons.ic_earth_24: ImageVector
    get() {
        if (_ic_earth_24 != null) return _ic_earth_24!!
        _ic_earth_24 = ImageVector.Builder(
            name = "ic_earth_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C17.5343 2 22 6.46572 22 12C22 17.5343 17.5343 22 12 22C6.46572 22 2 17.5343 2 12C2 6.46572 6.46572 2 12 2ZM12 4C10.6676 4 9.41365 4.32425 8.31055 4.89648L9.87891 6.46484C10.5967 7.18259 11 8.15696 11 9.17188C11 11.2861 9.28613 13 7.17188 13H4.06348C4.55326 16.9548 7.90913 20 12 20C13.8846 20 15.6118 19.3513 16.9766 18.2676L15.6826 16.3281C15.5628 16.1485 15.3728 16.0321 15.1631 16.0059L15.0723 16L14.8789 15.9932C13.9196 15.9254 13.0613 15.3568 12.627 14.4883L12.4932 14.2227C12.1085 13.4532 12.1084 12.5458 12.4932 11.7764L13.627 9.51074C14.0899 8.58423 15.0377 8 16.0723 8H18.9326C17.5525 5.60654 14.9704 4 12 4ZM16.0723 10C15.7934 10 15.5394 10.1577 15.416 10.4043L15.415 10.4053L14.2822 12.6709C14.179 12.8773 14.1791 13.1217 14.2822 13.3281L14.415 13.5938C14.5398 13.8433 14.7944 14 15.0723 14C15.9868 14.0001 16.8386 14.4579 17.3457 15.2178L18.4033 16.8027C19.4057 15.4665 20 13.8051 20 12C20 11.309 19.9135 10.6389 19.75 10H16.0723ZM6.63965 6.05371C5.24993 7.3066 4.306 9.04176 4.06348 11H7.17188C8.18156 11 8.99996 10.1816 9 9.17188C9 8.68679 8.8071 8.22116 8.46484 7.87891L6.63965 6.05371Z"),
            )
        }.build()
        return _ic_earth_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcEarth24Preview() {
    Icon(
        imageVector = Icons.ic_earth_24,
        contentDescription = null,
    )
}