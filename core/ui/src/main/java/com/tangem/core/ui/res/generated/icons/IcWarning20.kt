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

private var _ic_warning_20: ImageVector? = null

val Icons.ic_warning_20: ImageVector
    get() {
        if (_ic_warning_20 != null) return _ic_warning_20!!
        _ic_warning_20 = ImageVector.Builder(
            name = "ic_warning_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99995 12.0004C10.5522 12.0005 11 12.4482 11 13.0004C10.9998 13.5525 10.5521 14.0003 9.99995 14.0004C9.44776 14.0004 9.0001 13.5526 8.99995 13.0004C8.99995 12.4481 9.44767 12.0004 9.99995 12.0004Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99995 6.5004C10.414 6.50053 10.7499 6.83634 10.75 7.2504V10.2074C10.7497 10.6214 10.4139 10.9573 9.99995 10.9574C9.58586 10.9574 9.25015 10.6215 9.24995 10.2074V7.2504C9.25004 6.83626 9.58579 6.5004 9.99995 6.5004Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.96968 3.47306C8.96267 1.98102 11.2141 2.03071 12.123 3.62247L17.6767 13.3422C18.6069 14.9715 17.4302 16.9992 15.5537 16.9994H4.44624C2.56891 16.9994 1.39216 14.9711 2.32417 13.3412L7.87788 3.62247L7.96968 3.47306ZM10.8203 4.36661C10.4577 3.73174 9.54227 3.73181 9.17964 4.36661L3.62593 14.0863C3.26602 14.7159 3.72055 15.4994 4.44624 15.4994H15.5537C16.278 15.4992 16.7332 14.7162 16.374 14.0863L10.8203 4.36661Z"),
            )
        }.build()
        return _ic_warning_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWarning20Preview() {
    Icon(
        imageVector = Icons.ic_warning_20,
        contentDescription = null,
    )
}