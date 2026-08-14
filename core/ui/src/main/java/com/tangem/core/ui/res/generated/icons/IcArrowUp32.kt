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

private var _ic_arrow_up_32: ImageVector? = null

val Icons.ic_arrow_up_32: ImageVector
    get() {
        if (_ic_arrow_up_32 != null) return _ic_arrow_up_32!!
        _ic_arrow_up_32 = ImageVector.Builder(
            name = "ic_arrow_up_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.5003 27.3333C14.5003 28.1617 15.1719 28.8333 16.0003 28.8333C16.8286 28.8331 17.5003 28.1616 17.5003 27.3333V8.95538L25.6058 17.0609C26.1915 17.6465 27.1411 17.6463 27.7269 17.0609C28.3127 16.4751 28.3127 15.5255 27.7269 14.9398L17.0609 4.27277C16.4752 3.68703 15.5256 3.68714 14.9398 4.27277L4.2728 14.9398C3.68717 15.5256 3.68706 16.4751 4.2728 17.0609C4.85854 17.6464 5.80815 17.6464 6.39389 17.0609L14.5003 8.95441V27.3333Z"),
            )
        }.build()
        return _ic_arrow_up_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUp32Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_32,
        contentDescription = null,
    )
}