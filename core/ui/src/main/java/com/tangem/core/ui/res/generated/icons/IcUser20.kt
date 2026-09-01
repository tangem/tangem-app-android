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

private var _ic_user_20: ImageVector? = null

val Icons.ic_user_20: ImageVector
    get() {
        if (_ic_user_20 != null) return _ic_user_20!!
        _ic_user_20 = ImageVector.Builder(
            name = "ic_user_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.9922 11.8122C14.3706 11.8122 16.1797 13.7217 16.1797 15.9997C16.1795 16.4138 15.8438 16.7497 15.4297 16.7497C15.0157 16.7496 14.6799 16.4137 14.6797 15.9997C14.6797 14.5278 13.52 13.3122 11.9922 13.3122H8.00781C6.53586 13.3123 5.32034 14.4719 5.32031 15.9997C5.32012 16.4138 4.98441 16.7497 4.57031 16.7497C4.15622 16.7497 3.82051 16.4138 3.82031 15.9997C3.82034 13.6213 5.72979 11.8123 8.00781 11.8122H11.9922Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.18164 3.89133C8.79858 2.36958 11.2806 2.36953 12.8975 3.89133L12.9131 3.90695C14.5244 5.51869 14.5489 8.11591 12.8975 9.67062C11.3117 11.1631 8.72301 11.3098 7.14941 9.6384C5.65706 8.05254 5.50988 5.46481 7.18164 3.89133ZM11.8574 4.97336C10.8181 4.00579 9.24559 4.00881 8.20996 4.98312C7.22557 5.90961 7.23472 7.54062 8.24219 8.61105C9.16879 9.59461 10.799 9.58596 11.8691 8.57883C12.8716 7.63528 12.8961 6.01797 11.8574 4.97336Z"),
            )
        }.build()
        return _ic_user_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser20Preview() {
    Icon(
        imageVector = Icons.ic_user_20,
        contentDescription = null,
    )
}