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

private var _ic_arrow_swap_horizontal_24: ImageVector? = null

val Icons.ic_arrow_swap_horizontal_24: ImageVector
    get() {
        if (_ic_arrow_swap_horizontal_24 != null) return _ic_arrow_swap_horizontal_24!!
        _ic_arrow_swap_horizontal_24 = ImageVector.Builder(
            name = "ic_arrow_swap_horizontal_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21 11.0045C21.5523 11.0045 22 11.4522 22 12.0045C22 16.3821 18.7551 19.5045 14.5 19.5045H5.21191C5.49969 19.743 5.79278 19.9745 6.0918 20.1988C6.53636 20.5262 6.6328 21.1526 6.30566 21.5972C5.96497 22.0598 5.32136 22.1191 4.87402 21.7857C4.43528 21.4587 4.00784 21.1151 3.59473 20.7564C3.26986 20.4744 2.91892 20.1493 2.6416 19.8355C2.50393 19.6797 2.36275 19.5024 2.25098 19.317C2.15573 19.1589 2 18.8651 2 18.5045C2.00003 18.1439 2.15574 17.85 2.25098 17.692C2.36275 17.5066 2.50394 17.3292 2.6416 17.1734C2.91892 16.8596 3.26988 16.5346 3.59473 16.2525C4.00784 15.8938 4.43528 15.5502 4.87402 15.2232C4.88489 15.2151 4.8976 15.2084 4.90723 15.1988C5.35185 14.8717 5.97725 14.9672 6.30469 15.4117C6.63219 15.8564 6.53744 16.4826 6.09277 16.8101L6.0918 16.8092C5.79136 17.0311 5.4995 17.2661 5.21191 17.5045H14.5C17.6714 17.5045 20 15.2568 20 12.0045C20 11.4522 20.4477 11.0045 21 11.0045Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.6953 2.41169C18.036 1.94929 18.6787 1.88996 19.126 2.22321C19.5647 2.55021 19.9922 2.89383 20.4053 3.25251C20.7301 3.53456 21.0811 3.85962 21.3584 4.17341C21.4961 4.32922 21.6373 4.50657 21.749 4.69196C21.8443 4.84998 22 5.14386 22 5.50446C22 5.86506 21.8443 6.15891 21.749 6.31696C21.6373 6.50237 21.4961 6.67971 21.3584 6.83552C21.0811 7.14932 20.7301 7.47436 20.4053 7.75642C20.0754 8.04283 19.7486 8.30528 19.5059 8.4947C19.3736 8.59794 19.212 8.69092 19.0928 8.81013C18.6481 9.13731 18.0218 9.04181 17.6943 8.59724C17.3672 8.15256 17.4627 7.52621 17.9072 7.1988C18.2219 7.01 18.5075 6.73711 18.7881 6.50446H9.5C6.32862 6.50446 4.00003 8.75217 4 12.0045C4 12.5567 3.55228 13.0045 3 13.0045C2.44772 13.0045 2 12.5567 2 12.0045C2.00004 7.6269 5.24487 4.50446 9.5 4.50446H18.7881C18.5017 4.26707 18.2111 4.0345 17.9121 3.81306C17.4729 3.48759 17.3691 2.85468 17.6953 2.41169Z"),
            )
        }.build()
        return _ic_arrow_swap_horizontal_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowSwapHorizontal24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_swap_horizontal_24,
        contentDescription = null,
    )
}