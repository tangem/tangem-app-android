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

private var _ic_user_12_filled: ImageVector? = null

val Icons.ic_user_12_filled: ImageVector
    get() {
        if (_ic_user_12_filled != null) return _ic_user_12_filled!!
        _ic_user_12_filled = ImageVector.Builder(
            name = "ic_user_12_filled",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.10059 7.07511C8.46946 7.07511 9.4999 8.18772 9.5 9.49992C9.49993 9.776 9.2761 9.99991 9 9.99992H3C2.7239 9.99992 2.50007 9.77601 2.5 9.49992C2.5001 8.12904 3.58845 7.07511 4.89941 7.07511H7.10059Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.35156 2.66301C5.27849 1.77899 6.70491 1.779 7.63184 2.66301C7.63544 2.66644 7.6391 2.67022 7.64258 2.67375C8.56222 3.60602 8.5779 5.10892 7.63184 6.01164C6.7257 6.87583 5.23688 6.96662 4.33008 5.99015C3.48063 5.07518 3.3925 3.57808 4.35156 2.66301Z"),
            )
        }.build()
        return _ic_user_12_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser12FilledPreview() {
    Icon(
        imageVector = Icons.ic_user_12_filled,
        contentDescription = null,
    )
}