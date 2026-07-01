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

private var _ic_headphones_support_20: ImageVector? = null

val Icons.ic_headphones_support_20: ImageVector
    get() {
        if (_ic_headphones_support_20 != null) return _ic_headphones_support_20!!
        _ic_headphones_support_20 = ImageVector.Builder(
            name = "ic_headphones_support_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.002 2.5C12.9815 2.5002 15.4326 4.79461 15.6152 7.71094C16.6797 7.88185 17.5048 8.79245 17.5049 9.91016V12.1299C17.5048 13.3714 16.4874 14.3594 15.2539 14.3594C15.2488 15.5964 14.234 16.5798 13.0039 16.5801H11.9795C11.7006 17.1308 11.1261 17.5047 10.4717 17.5049H9.5332C8.61135 17.5046 7.84584 16.7643 7.8457 15.8301C7.84582 14.8958 8.61134 14.1555 9.5332 14.1553H10.4717C11.1262 14.1554 11.7006 14.5292 11.9795 15.0801H13.0039C13.4279 15.0798 13.7536 14.744 13.7539 14.3506V14.1572C13.3087 13.9011 13.004 13.4255 13.0039 12.8701V9.16992C13.004 8.47484 13.4797 7.90254 14.1133 7.73242C13.9438 5.65156 12.1775 4.00019 10.002 4C7.82634 4.00013 6.05918 5.6515 5.88965 7.73242C6.524 7.90201 7.00086 8.47426 7.00098 9.16992V12.8701C7.00078 13.7022 6.31925 14.3591 5.50098 14.3594H4.75C3.51677 14.3591 2.50007 13.3713 2.5 12.1299V9.91016C2.50012 8.79321 3.32336 7.88274 4.38672 7.71094C4.56939 4.79453 7.02228 2.50013 10.002 2.5ZM9.5332 15.6553C9.42036 15.6555 9.34582 15.7435 9.3457 15.8301C9.34584 15.9166 9.42037 16.0046 9.5332 16.0049H10.4717C10.5846 16.0047 10.659 15.9166 10.6592 15.8301C10.6591 15.7435 10.5846 15.6555 10.4717 15.6553H9.5332ZM4.75 9.17969C4.32578 9.17994 4.00014 9.51659 4 9.91016V12.1299C4.00007 12.5235 4.32574 12.8591 4.75 12.8594H5.50098V9.17969H4.75ZM14.5039 12.8594H15.2539C15.6784 12.8594 16.0048 12.5237 16.0049 12.1299V9.91016C16.0047 9.51643 15.6784 9.17969 15.2539 9.17969H14.5039V12.8594Z"),
            )
        }.build()
        return _ic_headphones_support_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeadphonesSupport20Preview() {
    Icon(
        imageVector = Icons.ic_headphones_support_20,
        contentDescription = null,
    )
}