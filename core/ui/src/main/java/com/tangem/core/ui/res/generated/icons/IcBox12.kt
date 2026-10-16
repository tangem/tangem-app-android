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

private var _ic_box_12: ImageVector? = null

val Icons.ic_box_12: ImageVector
    get() {
        if (_ic_box_12 != null) return _ic_box_12!!
        _ic_box_12 = ImageVector.Builder(
            name = "ic_box_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.4707 1.10524C5.86614 0.941764 6.31897 0.968323 6.69531 1.1863L9.80664 2.99001C10.237 3.23927 10.5 3.70004 10.5 4.19606V7.80348C10.5 8.30007 10.2359 8.75939 9.80566 9.00856L9.80664 9.00954L6.69531 10.8132C6.26522 11.0623 5.73478 11.0623 5.30469 10.8132L2.19336 9.00954C1.76392 8.76004 1.50015 8.29999 1.5 7.80348V4.19606C1.5 3.69956 1.76331 3.23924 2.19336 2.99001L5.30469 1.1863L5.4707 1.10524ZM2.5 7.80348C2.50015 7.94476 2.57498 8.07433 2.69531 8.1443L5.5 9.7693V6.28786L2.5 4.5486V7.80348ZM6.5 6.28786V9.7693L9.30469 8.1443L9.38574 8.0818C9.45766 8.00949 9.49997 7.90984 9.5 7.80348V4.5486L6.5 6.28786ZM2.99609 3.67946L6 5.42067L7.00195 4.83962L3.99902 3.0984L2.99609 3.67946ZM6.19336 2.05153C6.07353 1.98239 5.92647 1.98239 5.80664 2.05153L4.99609 2.52028L7.99902 4.26149L9.00293 3.67946L6.19336 2.05153Z"),
            )
        }.build()
        return _ic_box_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBox12Preview() {
    Icon(
        imageVector = Icons.ic_box_12,
        contentDescription = null,
    )
}