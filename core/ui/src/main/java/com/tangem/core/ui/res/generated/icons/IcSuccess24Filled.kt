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

private var _ic_success_24_filled: ImageVector? = null

val Icons.ic_success_24_filled: ImageVector
    get() {
        if (_ic_success_24_filled != null) return _ic_success_24_filled!!
        _ic_success_24_filled = ImageVector.Builder(
            name = "ic_success_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.9961 11.9961C21.996 6.4735 17.5187 1.99618 11.9961 1.99609C6.47353 1.99623 1.99623 6.47353 1.99609 11.9961C1.99624 17.5186 6.47354 21.996 11.9961 21.9961C17.5187 21.996 21.9959 17.5187 21.9961 11.9961ZM16.1888 8.89281C16.579 9.2832 16.5791 9.916 16.1888 10.3063L11.4387 15.0554C11.4027 15.1095 11.3614 15.162 11.3137 15.2097C10.9234 15.5995 10.2905 15.5996 9.90023 15.2097L7.73311 13.0426C7.3429 12.6523 7.34322 12.0195 7.73311 11.629C8.12348 11.2387 8.75625 11.2387 9.14662 11.629L10.5923 13.0748L14.7753 8.89281C15.1657 8.50244 15.7984 8.50245 16.1888 8.89281Z"),
            )
        }.build()
        return _ic_success_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSuccess24FilledPreview() {
    Icon(
        imageVector = Icons.ic_success_24_filled,
        contentDescription = null,
    )
}