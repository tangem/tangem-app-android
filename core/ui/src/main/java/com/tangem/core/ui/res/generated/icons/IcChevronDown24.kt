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

private var _ic_chevron_down_24: ImageVector? = null

val Icons.ic_chevron_down_24: ImageVector
    get() {
        if (_ic_chevron_down_24 != null) return _ic_chevron_down_24!!
        _ic_chevron_down_24 = ImageVector.Builder(
            name = "ic_chevron_down_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.291 8.79205C18.6815 8.40174 19.3146 8.40171 19.7051 8.79205C20.0949 9.18257 20.0952 9.8158 19.7051 10.2061L12.706 17.2051C12.5187 17.3924 12.2639 17.4979 11.999 17.4981C11.7342 17.498 11.4794 17.3922 11.292 17.2051L4.29296 10.2061C3.90253 9.81562 3.90257 9.18257 4.29296 8.79205C4.68349 8.40172 5.31656 8.40163 5.70703 8.79205L11.999 15.084L18.291 8.79205Z"),
            )
        }.build()
        return _ic_chevron_down_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDown24Preview() {
    Icon(
        imageVector = Icons.ic_chevron_down_24,
        contentDescription = null,
    )
}