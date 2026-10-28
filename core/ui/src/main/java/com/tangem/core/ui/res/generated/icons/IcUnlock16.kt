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

private var _ic_unlock_16: ImageVector? = null

val Icons.ic_unlock_16: ImageVector
    get() {
        if (_ic_unlock_16 != null) return _ic_unlock_16!!
        _ic_unlock_16 = ImageVector.Builder(
            name = "ic_unlock_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.00098 3C9.38347 3.00036 10.5535 4.07799 10.5537 5.4668V6.23047C11.3424 6.30006 11.9999 6.93862 12 7.76953V11.4541C11.9999 12.3346 11.2615 12.9998 10.4111 13H5.58887C4.73849 12.9998 4.00012 12.3346 4 11.4541V7.76953C4.00009 6.88899 4.73848 6.22385 5.58887 6.22363H9.30371V5.4668C9.30353 4.82171 8.74771 4.25036 8.00098 4.25C7.37462 4.25 6.87436 4.65742 6.73633 5.16895C6.64654 5.50213 6.30291 5.69996 5.96973 5.61035C5.63665 5.52039 5.43951 5.17692 5.5293 4.84375C5.81971 3.76662 6.83042 3 8.00098 3ZM5.58887 7.47363C5.37429 7.47384 5.2501 7.63266 5.25 7.76953V11.4541C5.25012 11.591 5.37431 11.7498 5.58887 11.75H10.4111C10.6257 11.7498 10.7499 11.591 10.75 11.4541V7.76953C10.7499 7.63266 10.6257 7.47384 10.4111 7.47363H5.58887Z"),
            )
        }.build()
        return _ic_unlock_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUnlock16Preview() {
    Icon(
        imageVector = Icons.ic_unlock_16,
        contentDescription = null,
    )
}