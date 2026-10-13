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

private var _ic_book_opened_28: ImageVector? = null

val Icons.ic_book_opened_28: ImageVector
    get() {
        if (_ic_book_opened_28 != null) return _ic_book_opened_28!!
        _ic_book_opened_28 = ImageVector.Builder(
            name = "ic_book_opened_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.66602 3.5C11.1483 3.5 12.5689 4.09138 13.6152 5.14258C13.7514 5.27941 13.8787 5.42347 13.999 5.57227C15.0216 4.30858 16.5822 3.5 18.334 3.5H22.126C22.89 3.5 23.6217 3.80481 24.1602 4.3457C24.6985 4.88659 25 5.61946 25 6.38184V19.4414C25 21.0247 23.7229 22.3194 22.1328 22.3232H17.5098C16.5012 22.3218 15.5748 22.8944 15.1211 23.8066C14.8627 24.3261 14.2806 24.5995 13.7158 24.4668C13.3435 24.3793 13.0433 24.1303 12.8818 23.8047L12.8789 23.8066C12.4252 22.8944 11.4988 22.3218 10.4902 22.3232H5.87402C5.10993 22.3232 4.37836 22.0185 3.83984 21.4775C3.30151 20.9367 3 20.2038 3 19.4414V6.38184C3 5.61946 3.30151 4.88659 3.83984 4.3457C4.37835 3.8048 5.11002 3.5 5.87402 3.5H9.66602ZM5.87402 6C5.77674 6 5.68192 6.03845 5.61133 6.10938C5.54053 6.18051 5.5 6.27868 5.5 6.38184V19.4414C5.5 19.5445 5.54062 19.6427 5.61133 19.7139C5.68193 19.7848 5.77674 19.8232 5.87402 19.8232H10.4883C11.287 19.8223 12.0563 20.0088 12.748 20.3457V9.10254C12.748 8.27782 12.4224 7.48763 11.8438 6.90625C11.2653 6.32505 10.4815 6 9.66602 6H5.87402ZM18.334 6C16.6374 6.00001 15.252 7.38348 15.252 9.10254V20.3457C15.9437 20.0088 16.713 19.8223 17.5117 19.8232H22.127C22.3276 19.8227 22.5 19.6574 22.5 19.4414V6.38184C22.5 6.27868 22.4595 6.18051 22.3887 6.10938C22.3181 6.03846 22.2233 6 22.126 6H18.334Z"),
            )
        }.build()
        return _ic_book_opened_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBookOpened28Preview() {
    Icon(
        imageVector = Icons.ic_book_opened_28,
        contentDescription = null,
    )
}