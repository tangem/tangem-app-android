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

private var _ic_logo_tangem_20: ImageVector? = null

val Icons.ic_logo_tangem_20: ImageVector
    get() {
        if (_ic_logo_tangem_20 != null) return _ic_logo_tangem_20!!
        _ic_logo_tangem_20 = ImageVector.Builder(
            name = "ic_logo_tangem_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.33301 9.75977V16H7.37012C6.54036 16 6.12589 15.9998 5.80859 15.8447C5.52981 15.709 5.30342 15.4913 5.16113 15.2236C4.99971 14.919 5 14.521 5 13.7246V9.75977H8.33301Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15 13.7246C15 14.5212 14.9994 14.919 14.8379 15.2236C14.6956 15.4912 14.4701 15.7091 14.1914 15.8447C13.8741 15.9998 13.4596 16 12.6299 16H11.667V10.2031L11.666 9.75977H15V13.7246Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.6299 4C13.4596 4 13.8741 4.0002 14.1914 4.15527C14.4702 4.29097 14.6966 4.50872 14.8389 4.77637C15.0003 5.08095 15 5.47896 15 6.27539V7.12012H5V6.27539C5 5.47896 4.99971 5.08095 5.16113 4.77637C5.30246 4.50872 5.52981 4.29189 5.80859 4.15527C6.12589 4.0002 6.54036 4 7.37012 4H12.6299Z"),
            )
        }.build()
        return _ic_logo_tangem_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLogoTangem20Preview() {
    Icon(
        imageVector = Icons.ic_logo_tangem_20,
        contentDescription = null,
    )
}