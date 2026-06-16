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

private var _ic_success_24: ImageVector? = null

val Icons.ic_success_24: ImageVector
    get() {
        if (_ic_success_24 != null) return _ic_success_24!!
        _ic_success_24 = ImageVector.Builder(
            name = "ic_success_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.7803 8.89551C15.1708 8.50499 15.8038 8.50499 16.1943 8.89551C16.5847 9.28604 16.5848 9.9191 16.1943 10.3096L11.4424 15.0605C11.4063 15.1146 11.3651 15.1672 11.3174 15.2148C10.9269 15.6048 10.2937 15.6049 9.90332 15.2148L7.73535 13.0469C7.34499 12.6565 7.34531 12.0234 7.73535 11.6328C8.12588 11.2423 8.75889 11.2423 9.14941 11.6328L10.5957 13.0791L14.7803 8.89551Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 1.99609C17.5248 1.99618 22.0038 6.47525 22.0039 12C22.0038 17.5247 17.5248 22.0038 12 22.0039C6.47529 22.0038 1.99624 17.5247 1.99609 12C1.99623 6.47528 6.47528 1.99623 12 1.99609ZM12 3.99609C7.57984 3.99623 3.99623 7.57984 3.99609 12C3.99624 16.4201 7.57986 20.0038 12 20.0039C16.4202 20.0038 20.0038 16.4202 20.0039 12C20.0038 7.57982 16.4202 3.99618 12 3.99609Z"),
            )
        }.build()
        return _ic_success_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSuccess24Preview() {
    Icon(
        imageVector = Icons.ic_success_24,
        contentDescription = null,
    )
}