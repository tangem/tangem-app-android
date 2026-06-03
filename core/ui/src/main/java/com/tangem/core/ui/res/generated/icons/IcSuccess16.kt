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

private var _ic_success_16: ImageVector? = null

val Icons.ic_success_16: ImageVector
    get() {
        if (_ic_success_16 != null) return _ic_success_16!!
        _ic_success_16 = ImageVector.Builder(
            name = "ic_success_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.7207 6.07129C9.96472 5.82766 10.3605 5.82759 10.6045 6.07129C10.8486 6.31535 10.8485 6.71197 10.6045 6.95605L7.65625 9.90332C7.63429 9.93582 7.60979 9.96731 7.58105 9.99609C7.36741 10.2097 7.03754 10.2365 6.79492 10.0762L6.69727 9.99609L6.68848 9.98828C6.68683 9.9866 6.68522 9.98409 6.68359 9.98242L5.32617 8.62598C5.08257 8.38192 5.0824 7.98614 5.32617 7.74219C5.57022 7.49818 5.96686 7.49821 6.21094 7.74219L7.12988 8.66113L9.7207 6.07129Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 1.99902C11.3136 1.99922 13.9996 4.68645 14 8C13.9998 11.3138 11.3138 13.9998 8 14C4.68642 13.9996 1.99922 11.3136 1.99902 8C1.99946 4.68658 4.68657 1.99944 8 1.99902ZM8 3.24902C5.37693 3.24944 3.24946 5.37694 3.24902 8C3.24922 10.6233 5.37678 12.7496 8 12.75C10.6234 12.7498 12.7498 10.6234 12.75 8C12.7496 5.3768 10.6233 3.24922 8 3.24902Z"),
            )
        }.build()
        return _ic_success_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSuccess16Preview() {
    Icon(
        imageVector = Icons.ic_success_16,
        contentDescription = null,
    )
}