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

private var _ic_sign_plus_24: ImageVector? = null

val Icons.ic_sign_plus_24: ImageVector
    get() {
        if (_ic_sign_plus_24 != null) return _ic_sign_plus_24!!
        _ic_sign_plus_24 = ImageVector.Builder(
            name = "ic_sign_plus_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.0049 4.00488C12.5571 4.00488 13.0048 4.45265 13.0049 5.00488V11H19C19.5523 11 20 11.4477 20 12C20 12.5523 19.5523 13 19 13H13.0049V19.0049C13.0049 19.5572 12.5572 20.0049 12.0049 20.0049C11.4527 20.0048 11.0049 19.5571 11.0049 19.0049V13H5C4.44772 13 4 12.5523 4 12C4 11.4477 4.44772 11 5 11H11.0049V5.00488C11.0049 4.45269 11.4527 4.00495 12.0049 4.00488Z"),
            )
        }.build()
        return _ic_sign_plus_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignPlus24Preview() {
    Icon(
        imageVector = Icons.ic_sign_plus_24,
        contentDescription = null,
    )
}