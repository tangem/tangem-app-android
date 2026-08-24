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

private var _ic_shield_checkmark_24_filled: ImageVector? = null

val Icons.ic_shield_checkmark_24_filled: ImageVector
    get() {
        if (_ic_shield_checkmark_24_filled != null) return _ic_shield_checkmark_24_filled!!
        _ic_shield_checkmark_24_filled = ImageVector.Builder(
            name = "ic_shield_checkmark_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.9473 3.39122C12.6995 2.86961 11.2995 2.86957 10.0518 3.39122L5.60742 5.24766C4.55741 5.68573 3.8877 6.72088 3.8877 7.85118V11.0895C3.88806 15.6757 7.24261 19.9356 11.7764 20.9762C11.9235 21.0099 12.0765 21.0099 12.2236 20.9762C16.7573 19.9356 20.1119 15.6757 20.1123 11.0895V7.85118C20.1123 6.71984 19.4413 5.6863 18.3926 5.24766L13.9473 3.39122ZM15.6836 9.2711C16.0862 9.64881 16.107 10.2813 15.7295 10.6842L11.9795 14.6842C11.7905 14.8857 11.5263 15.0006 11.25 15.0006C10.9737 15.0006 10.7095 14.8857 10.5205 14.6842L8.27051 12.2848C7.89285 11.8819 7.91371 11.2485 8.31641 10.8707C8.71932 10.4933 9.35187 10.5139 9.72949 10.9166L11.25 12.5387L14.2705 9.317C14.6482 8.91414 15.2807 8.89353 15.6836 9.2711Z"),
            )
        }.build()
        return _ic_shield_checkmark_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShieldCheckmark24FilledPreview() {
    Icon(
        imageVector = Icons.ic_shield_checkmark_24_filled,
        contentDescription = null,
    )
}