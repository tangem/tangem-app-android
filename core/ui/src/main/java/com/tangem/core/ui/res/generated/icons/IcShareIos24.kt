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

private var _ic_share_ios_24: ImageVector? = null

val Icons.ic_share_ios_24: ImageVector
    get() {
        if (_ic_share_ios_24 != null) return _ic_share_ios_24!!
        _ic_share_ios_24 = ImageVector.Builder(
            name = "ic_share_ios_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.5 8C9.05228 8 9.5 8.44772 9.5 9C9.5 9.55228 9.05228 10 8.5 10H7.5C6.94775 10 6.5 10.4477 6.5 11V18.5C6.5 19.0523 6.94772 19.5 7.5 19.5H16.5C17.0523 19.5 17.5 19.0523 17.5 18.5V11C17.5 10.4477 17.0523 10 16.5 10H15.5C14.9477 10 14.5 9.55228 14.5 9C14.5 8.44772 14.9477 8 15.5 8H16.5C18.1569 8 19.5 9.34315 19.5 11V18.5C19.5 20.1569 18.1569 21.5 16.5 21.5H7.5C5.84315 21.5 4.5 20.1568 4.5 18.5V11C4.50001 9.34318 5.84319 8.00004 7.5 8H8.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C12.2651 2.00004 12.5195 2.10553 12.707 2.29297L15.2109 4.79688C15.6012 5.18739 15.6013 5.82049 15.2109 6.21094C14.8205 6.60129 14.1874 6.6012 13.7969 6.21094L13 5.41406V13.5C12.9999 14.0522 12.5522 14.5 12 14.5C11.4478 14.5 11.0001 14.0522 11 13.5V5.41406L10.2031 6.21094C9.81261 6.60127 9.17954 6.60133 8.78906 6.21094C8.39869 5.82046 8.39874 5.18738 8.78906 4.79688L11.293 2.29297L11.3662 2.22656C11.5441 2.08081 11.768 2.00004 12 2Z"),
            )
        }.build()
        return _ic_share_ios_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShareIos24Preview() {
    Icon(
        imageVector = Icons.ic_share_ios_24,
        contentDescription = null,
    )
}