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

private var _ic_arrow_right_left_28: ImageVector? = null

val Icons.ic_arrow_right_left_28: ImageVector
    get() {
        if (_ic_arrow_right_left_28 != null) return _ic_arrow_right_left_28!!
        _ic_arrow_right_left_28 = ImageVector.Builder(
            name = "ic_arrow_right_left_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.56738 13.4719C9.05556 12.984 9.84692 12.9839 10.335 13.4719C10.8226 13.9601 10.8229 14.7515 10.335 15.2395L8.26953 17.3039H22.75C23.4402 17.3041 24 17.8636 24 18.5539C23.9999 19.2441 23.4402 19.8038 22.75 19.8039H8.26953L10.335 21.8693C10.8227 22.3575 10.8228 23.1489 10.335 23.6369C9.8469 24.1248 9.05551 24.1247 8.56738 23.6369L4.36816 19.4377C4.13385 19.2034 4.00208 18.8853 4.00195 18.5539C4.00198 18.2223 4.13368 17.9035 4.36816 17.6691L8.56738 13.4719Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.666 4.36543C18.1542 3.87743 18.9455 3.87733 19.4336 4.36543L23.6123 8.54414C23.7635 8.68882 23.8787 8.87053 23.9424 9.07539C23.9472 9.09075 23.9499 9.10671 23.9541 9.12227C23.9613 9.14909 23.9692 9.17581 23.9746 9.20332C23.9906 9.28333 23.999 9.36566 23.999 9.44844C23.9989 9.77985 23.8672 10.0989 23.6328 10.3332L19.4336 14.5305C18.9455 15.0182 18.1541 15.0182 17.666 14.5305C17.178 14.0423 17.1781 13.251 17.666 12.7629L19.7334 10.6965H5.25098C4.56075 10.6963 4.00098 10.1367 4.00098 9.44649C4.00115 8.75637 4.56086 8.19664 5.25098 8.19649H19.7295L17.666 6.13301C17.1779 5.64492 17.1781 4.8536 17.666 4.36543Z"),
            )
        }.build()
        return _ic_arrow_right_left_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRightLeft28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_right_left_28,
        contentDescription = null,
    )
}