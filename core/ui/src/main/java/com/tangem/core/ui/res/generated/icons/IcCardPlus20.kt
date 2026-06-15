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

private var _ic_card_plus_20: ImageVector? = null

val Icons.ic_card_plus_20: ImageVector
    get() {
        if (_ic_card_plus_20 != null) return _ic_card_plus_20!!
        _ic_card_plus_20 = ImageVector.Builder(
            name = "ic_card_plus_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.8086 4.38867C11.2226 4.38894 11.5586 4.72464 11.5586 5.13867C11.5586 5.55272 11.2226 5.88841 10.8086 5.88867H5.16797C4.1867 5.88883 3.501 6.60642 3.50098 7.36133V14.0283C3.50121 14.7831 4.18685 15.4998 5.16797 15.5H14.8379C15.8187 15.4996 16.5046 14.783 16.5049 14.0283V9.58301C16.5051 9.16895 16.8408 8.83301 17.2549 8.83301C17.6687 8.8333 18.0047 9.16913 18.0049 9.58301V14.0283C18.0047 15.7275 16.526 16.9996 14.8379 17H5.16797C3.4796 16.9998 2.00023 15.7277 2 14.0283V7.36133C2.00002 5.66175 3.47949 4.38883 5.16797 4.38867H10.8086Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.63965 12.665C8.05353 12.6653 8.38951 13.0011 8.38965 13.415C8.3894 13.8289 8.05346 14.1648 7.63965 14.165H5.65137C5.23731 14.165 4.90162 13.829 4.90137 13.415C4.90151 13.0009 5.23724 12.665 5.65137 12.665H7.63965Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.3203 3C15.7345 3 16.0703 3.33579 16.0703 3.75V4.38867H16.7705C17.1846 4.38867 17.5202 4.72468 17.5205 5.13867C17.5205 5.55286 17.1847 5.88867 16.7705 5.88867H16.0703V6.52832C16.0699 6.94222 15.7343 7.27832 15.3203 7.27832C14.9064 7.27829 14.5707 6.9422 14.5703 6.52832V5.88867H13.8701C13.456 5.88854 13.1201 5.55278 13.1201 5.13867C13.1204 4.72476 13.4562 4.3888 13.8701 4.38867H14.5703V3.75C14.5703 3.33581 14.9061 3.00003 15.3203 3Z"),
            )
        }.build()
        return _ic_card_plus_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCardPlus20Preview() {
    Icon(
        imageVector = Icons.ic_card_plus_20,
        contentDescription = null,
    )
}