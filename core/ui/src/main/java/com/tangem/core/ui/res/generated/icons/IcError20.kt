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

private var _ic_error_20: ImageVector? = null

val Icons.ic_error_20: ImageVector
    get() {
        if (_ic_error_20 != null) return _ic_error_20!!
        _ic_error_20 = ImageVector.Builder(
            name = "ic_error_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.0957 12.376C10.5713 12.4242 10.9451 12.8246 10.9453 13.3154C10.9452 13.8371 10.5216 14.2605 10 14.2607C9.47839 14.2605 9.05576 13.8371 9.05566 13.3154C9.05504 12.791 9.48112 12.3711 10 12.3711L10.0957 12.376Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 5.73926C10.414 5.7395 10.75 6.07521 10.75 6.48926V10.3896C10.75 10.8037 10.414 11.1394 10 11.1396C9.58598 11.1394 9.25 10.8037 9.25 10.3896V6.48926C9.25002 6.0752 9.58599 5.73949 10 5.73926Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.5537 2.00195C13.1795 2.00199 13.7802 2.25102 14.2227 2.69336L17.3047 5.77539C17.7485 6.21806 17.998 6.81932 17.998 7.44531V12.5537C17.998 13.1795 17.749 13.7802 17.3066 14.2227L13.752 17.7783C13.6113 17.919 13.4196 17.998 13.2207 17.998H7.44531C6.81953 17.998 6.21879 17.7489 5.77637 17.3066L2.69336 14.2236C2.25101 13.7812 2.00204 13.1805 2.00195 12.5547V7.44531C2.00201 6.81948 2.25098 6.2188 2.69336 5.77637L5.77637 2.69336C6.21881 2.25099 6.81947 2.002 7.44531 2.00195H12.5537ZM7.44531 3.50195C7.21775 3.502 6.99836 3.59252 6.83691 3.75391L3.75391 6.83691C3.59252 6.99836 3.50201 7.21775 3.50195 7.44531V12.5547C3.50204 12.7822 3.59255 13.0017 3.75391 13.1631L6.83691 16.2451C6.99838 16.4066 7.21769 16.498 7.44531 16.498H12.9102L16.2451 13.1621C16.4066 13.0006 16.498 12.7813 16.498 12.5537V7.44531C16.498 7.2178 16.4068 6.99912 16.2451 6.83789L13.1621 3.75391C13.0007 3.59255 12.7813 3.50199 12.5537 3.50195H7.44531Z"),
            )
        }.build()
        return _ic_error_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcError20Preview() {
    Icon(
        imageVector = Icons.ic_error_20,
        contentDescription = null,
    )
}