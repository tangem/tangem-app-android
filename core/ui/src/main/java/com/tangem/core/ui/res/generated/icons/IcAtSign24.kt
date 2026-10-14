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

private var _ic_at_sign_24: ImageVector? = null

val Icons.ic_at_sign_24: ImageVector
    get() {
        if (_ic_at_sign_24 != null) return _ic_at_sign_24!!
        _ic_at_sign_24 = ImageVector.Builder(
            name = "ic_at_sign_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15 12C15 10.3433 13.6567 9 12 9C10.3433 9 9 10.3433 9 12C9 13.6567 10.3433 15 12 15C13.6567 15 15 13.6567 15 12ZM17 13.5C17 14.3287 17.6713 15 18.5 15C19.3287 15 20 14.3287 20 13.5V12C20 7.58128 16.4187 4 12 4C7.58128 4 4 7.58128 4 12C4 16.4187 7.58128 20 12 20C13.0218 20 14.0597 19.8067 15.0615 19.3916C15.5971 19.1696 16.0961 18.8891 16.5615 18.5654C17.0149 18.2501 17.6387 18.362 17.9541 18.8154C18.2691 19.2688 18.1574 19.8918 17.7041 20.207C17.1318 20.6051 16.5072 20.9574 15.8271 21.2393C14.575 21.7581 13.2762 22 12 22C6.47672 22 2 17.5233 2 12C2 6.47672 6.47672 2 12 2C17.5233 2 22 6.47672 22 12V13.5C22 15.4333 20.4333 17 18.5 17C17.2947 17 16.2321 16.3909 15.6025 15.4639C14.6928 16.4098 13.4159 17 12 17C9.23872 17 7 14.7613 7 12C7 9.23872 9.23872 7 12 7C14.7613 7 17 9.23872 17 12V13.5Z"),
            )
        }.build()
        return _ic_at_sign_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtSign24Preview() {
    Icon(
        imageVector = Icons.ic_at_sign_24,
        contentDescription = null,
    )
}