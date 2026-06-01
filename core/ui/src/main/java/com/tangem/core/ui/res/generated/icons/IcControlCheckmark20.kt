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

private var _ic_control_checkmark_20: ImageVector? = null

val Icons.ic_control_checkmark_20: ImageVector
    get() {
        if (_ic_control_checkmark_20 != null) return _ic_control_checkmark_20!!
        _ic_control_checkmark_20 = ImageVector.Builder(
            name = "ic_control_checkmark_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7091 7.47177C13.002 7.17905 13.4768 7.17896 13.7696 7.47177C14.0623 7.76462 14.0623 8.23946 13.7696 8.53232L9.80188 12.4991C9.77414 12.5414 9.74238 12.583 9.7052 12.6202C9.41232 12.9124 8.93736 12.9127 8.64465 12.6202L6.83801 10.8136C6.54535 10.5209 6.54572 10.046 6.83801 9.75302C7.13089 9.46014 7.60566 9.46016 7.89855 9.75302L9.1632 11.0177L12.7091 7.47177Z"),
            )
        }.build()
        return _ic_control_checkmark_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcControlCheckmark20Preview() {
    Icon(
        imageVector = Icons.ic_control_checkmark_20,
        contentDescription = null,
    )
}