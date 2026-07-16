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

private var _ic_arrow_refresh_20: ImageVector? = null

val Icons.ic_arrow_refresh_20: ImageVector
    get() {
        if (_ic_arrow_refresh_20 != null) return _ic_arrow_refresh_20!!
        _ic_arrow_refresh_20 = ImageVector.Builder(
            name = "ic_arrow_refresh_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.75 9.25C16.1642 9.25 16.5 9.58579 16.5 10C16.5 13.5902 13.5901 16.4999 10 16.5C7.98553 16.5 6.19017 15.5813 5 14.1445V14.791C5 15.2051 4.66404 15.5408 4.25 15.541C3.83581 15.541 3.5 15.2052 3.5 14.791V12.3955C3.50015 11.9814 3.8359 11.6455 4.25 11.6455H6.64551C7.05963 11.6455 7.39536 11.9814 7.39551 12.3955C7.39551 12.8097 7.05972 13.1455 6.64551 13.1455H6.12305C7.0395 14.2763 8.43424 15 10 15C12.7617 14.9999 15 12.7617 15 10C15 9.58584 15.3359 9.25008 15.75 9.25Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 3.5C12.0145 3.50009 13.8099 4.41862 15 5.85547V5.20801C15.0001 4.79393 15.3359 4.45809 15.75 4.45801C16.1641 4.45801 16.4999 4.79388 16.5 5.20801V7.60352C16.5 8.01773 16.1642 8.35352 15.75 8.35352H13.3545C12.9403 8.35343 12.6045 8.01768 12.6045 7.60352C12.6048 7.18962 12.9405 6.8536 13.3545 6.85352H13.876C12.9595 5.72327 11.5652 5.00009 10 5C7.23838 5.00014 5.00007 7.23837 5 10C4.99989 10.414 4.664 10.7498 4.25 10.75C3.83591 10.7499 3.50011 10.4141 3.5 10C3.50007 6.40994 6.40996 3.50014 10 3.5Z"),
            )
        }.build()
        return _ic_arrow_refresh_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRefresh20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_refresh_20,
        contentDescription = null,
    )
}