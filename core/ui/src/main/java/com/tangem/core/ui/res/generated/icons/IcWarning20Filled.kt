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

private var _ic_warning_20_filled: ImageVector? = null

val Icons.ic_warning_20_filled: ImageVector
    get() {
        if (_ic_warning_20_filled != null) return _ic_warning_20_filled!!
        _ic_warning_20_filled = ImageVector.Builder(
            name = "ic_warning_20_filled",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.96968 3.47306C8.96267 1.98102 11.2141 2.03071 12.123 3.62247L17.6767 13.3422C18.6069 14.9715 17.4302 16.9992 15.5537 16.9994H4.44624C2.56891 16.9994 1.39216 14.9711 2.32417 13.3412L7.87788 3.62247L7.96968 3.47306ZM9.99995 12.0004C9.44767 12.0004 8.99995 12.4481 8.99995 13.0004C9.0001 13.5526 9.44776 14.0004 9.99995 14.0004C10.5521 14.0003 10.9998 13.5525 11 13.0004C11 12.4482 10.5522 12.0005 9.99995 12.0004ZM10.75 7.2504C10.7499 6.83634 10.414 6.50053 9.99995 6.5004C9.58579 6.5004 9.25004 6.83626 9.24995 7.2504V10.2074C9.25015 10.6215 9.58586 10.9574 9.99995 10.9574C10.4139 10.9573 10.7497 10.6214 10.75 10.2074V7.2504Z"),
            )
        }.build()
        return _ic_warning_20_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWarning20FilledPreview() {
    Icon(
        imageVector = Icons.ic_warning_20_filled,
        contentDescription = null,
    )
}