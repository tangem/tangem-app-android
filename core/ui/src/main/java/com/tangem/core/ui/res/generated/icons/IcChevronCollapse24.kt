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

private var _ic_chevron_collapse_24: ImageVector? = null

val Icons.ic_chevron_collapse_24: ImageVector
    get() {
        if (_ic_chevron_collapse_24 != null) return _ic_chevron_collapse_24!!
        _ic_chevron_collapse_24 = ImageVector.Builder(
            name = "ic_chevron_collapse_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.2559 12.751C10.8071 12.7523 11.2529 13.2006 11.2529 13.752V20.7471C11.2528 21.2991 10.8059 21.7468 10.2539 21.7471C9.7017 21.7471 9.25404 21.2992 9.25391 20.7471V14.75L3.25586 14.7383C2.70385 14.7371 2.257 14.2893 2.25781 13.7373C2.25879 13.1853 2.70686 12.7377 3.25879 12.7383L10.2559 12.751ZM13.751 2.25781C14.3033 2.25781 14.751 2.70553 14.751 3.25781L14.752 9.25391H20.7471C21.2992 9.25407 21.7471 9.70172 21.7471 10.2539C21.7467 10.8058 21.299 11.2528 20.7471 11.2529H13.752C13.1997 11.2529 12.7511 10.8051 12.751 10.2529V3.25781C12.751 2.70561 13.1988 2.25795 13.751 2.25781Z"),
            )
        }.build()
        return _ic_chevron_collapse_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronCollapse24Preview() {
    Icon(
        imageVector = Icons.ic_chevron_collapse_24,
        contentDescription = null,
    )
}