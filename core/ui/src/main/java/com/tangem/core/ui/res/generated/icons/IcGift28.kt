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

private var _ic_gift_28: ImageVector? = null

val Icons.ic_gift_28: ImageVector
    get() {
        if (_ic_gift_28 != null) return _ic_gift_28!!
        _ic_gift_28 = ImageVector.Builder(
            name = "ic_gift_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.0332 3.58879C15.4293 2.13676 17.7143 2.13673 19.1104 3.58879C20.3189 4.84607 20.4598 6.77835 19.5352 8.19426H23.1113C24.4302 8.19442 25.4999 9.26414 25.5 10.5829V12.8612C25.4999 14.1427 24.4898 15.1857 23.2227 15.2441V23.1112C23.2227 24.43 22.1527 25.4996 20.834 25.4999H7.16699C5.84803 25.4998 4.77832 24.4302 4.77832 23.1112V15.2441C3.5107 15.1862 2.50011 14.143 2.5 12.8612V10.5829C2.50014 9.26412 3.56979 8.19437 4.88867 8.19426H8.46484C7.54007 6.77836 7.68107 4.84612 8.88965 3.58879C10.2743 2.14892 12.5319 2.13804 13.9307 3.55461C13.9428 3.56629 13.956 3.5775 13.9678 3.58976C13.979 3.60142 13.989 3.61412 14 3.6259C14.0109 3.6136 14.0217 3.60075 14.0332 3.58879ZM7.27832 15.2499V22.9999H20.7227V15.2499H7.27832ZM5 12.7499H23V10.6943H5V12.7499ZM17.3076 5.32219C16.8952 4.89336 16.2483 4.89335 15.8359 5.32219L15.8311 5.32512C15.6871 5.47672 15.5414 5.74532 15.4307 6.16301C15.3215 6.57497 15.2683 7.05175 15.2539 7.52824C15.2484 7.7103 15.249 7.88734 15.2529 8.05461C15.9664 7.89118 16.7595 7.56746 17.3076 6.99699L17.3105 6.99406C17.7466 6.53689 17.7456 5.77806 17.3076 5.32219ZM12.1641 5.32219C11.7517 4.89333 11.1038 4.89338 10.6914 5.32219C10.2544 5.77722 10.253 6.53374 10.6865 6.99113C10.6884 6.99304 10.6905 6.99507 10.6924 6.99699C11.24 7.56684 12.0322 7.89009 12.7451 8.05363C12.7491 7.88664 12.7516 7.70995 12.7461 7.52824C12.7317 7.05178 12.6795 6.57496 12.5703 6.16301C12.4595 5.74485 12.3121 5.47665 12.168 5.32512L12.1641 5.32219Z"),
            )
        }.build()
        return _ic_gift_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGift28Preview() {
    Icon(
        imageVector = Icons.ic_gift_28,
        contentDescription = null,
    )
}