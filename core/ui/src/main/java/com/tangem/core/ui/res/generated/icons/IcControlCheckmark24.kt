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

private var _ic_control_checkmark_24: ImageVector? = null

val Icons.ic_control_checkmark_24: ImageVector
    get() {
        if (_ic_control_checkmark_24 != null) return _ic_control_checkmark_24!!
        _ic_control_checkmark_24 = ImageVector.Builder(
            name = "ic_control_checkmark_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.7805 8.89449C15.171 8.50429 15.8041 8.5042 16.1946 8.89449C16.585 9.28492 16.5848 9.91801 16.1946 10.3086L11.4426 15.0595C11.4066 15.1136 11.3653 15.1661 11.3176 15.2138C10.9271 15.6042 10.2941 15.6042 9.90357 15.2138L7.7356 13.0459C7.34523 12.6554 7.3452 12.0223 7.7356 11.6318C8.1261 11.2416 8.75922 11.2415 9.14966 11.6318L10.5959 13.0781L14.7805 8.89449Z"),
            )
        }.build()
        return _ic_control_checkmark_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcControlCheckmark24Preview() {
    Icon(
        imageVector = Icons.ic_control_checkmark_24,
        contentDescription = null,
    )
}