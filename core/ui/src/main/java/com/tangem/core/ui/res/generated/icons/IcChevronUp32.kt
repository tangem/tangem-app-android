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

private var _ic_chevron_up_32: ImageVector? = null

val Icons.ic_chevron_up_32: ImageVector
    get() {
        if (_ic_chevron_up_32 != null) return _ic_chevron_up_32!!
        _ic_chevron_up_32 = ImageVector.Builder(
            name = "ic_chevron_up_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.0006 8.83521C16.3983 8.83524 16.7809 8.99347 17.0622 9.27466L25.5612 17.7747C26.1468 18.3604 26.1467 19.31 25.5612 19.8958C24.9754 20.4815 24.0259 20.4815 23.4401 19.8958L16.0006 12.4573L8.56215 19.8958C7.97637 20.4815 7.02685 20.4815 6.44106 19.8958C5.85543 19.31 5.85533 18.3604 6.44106 17.7747L14.9401 9.27466C15.2213 8.99361 15.6031 8.83532 16.0006 8.83521Z"),
            )
        }.build()
        return _ic_chevron_up_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronUp32Preview() {
    Icon(
        imageVector = Icons.ic_chevron_up_32,
        contentDescription = null,
    )
}