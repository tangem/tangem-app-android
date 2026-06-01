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

private var _ic_arrow_right_24: ImageVector? = null

val Icons.ic_arrow_right_24: ImageVector
    get() {
        if (_ic_arrow_right_24 != null) return _ic_arrow_right_24!!
        _ic_arrow_right_24 = ImageVector.Builder(
            name = "ic_arrow_right_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.2929 4.29263C13.6833 3.9029 14.3166 3.90283 14.7069 4.29263L21.706 11.2916C21.8932 11.4789 21.9987 11.7339 21.9989 11.9987C21.9989 12.2636 21.8931 12.5183 21.706 12.7057L14.7069 19.7047C14.3165 20.0951 13.6834 20.0951 13.2929 19.7047C12.9029 19.3142 12.9027 18.681 13.2929 18.2907L18.5849 12.9987H2.99991C2.44775 12.9986 1.99994 12.5509 1.99991 11.9987C2.00043 11.4469 2.44805 10.9988 2.99991 10.9987H18.5849L13.2929 5.70669C12.9029 5.31616 12.9027 4.68299 13.2929 4.29263Z"),
            )
        }.build()
        return _ic_arrow_right_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRight24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_right_24,
        contentDescription = null,
    )
}