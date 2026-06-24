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

private var _ic_trash_bin_28: ImageVector? = null

val Icons.ic_trash_bin_28: ImageVector
    get() {
        if (_ic_trash_bin_28 != null) return _ic_trash_bin_28!!
        _ic_trash_bin_28 = ImageVector.Builder(
            name = "ic_trash_bin_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.3135 2C17.9434 2.00012 19.2974 3.30677 19.2979 4.95801V5.98633H23.252C23.9419 5.98659 24.5016 6.54642 24.502 7.23633C24.502 7.92652 23.9421 8.48606 23.252 8.48633H22.7676V21.4727C22.7676 23.4387 21.1545 25.0006 19.2051 25.001H8.79688C6.84731 25.0008 5.23438 23.4388 5.23438 21.4727V8.48633H4.75C4.05964 8.48633 3.5 7.92668 3.5 7.23633C3.50033 6.54626 4.05985 5.98633 4.75 5.98633H8.70312V4.95801C8.70361 3.30676 10.0575 2.00011 11.6875 2H16.3135ZM7.73438 21.4727C7.73438 22.0224 8.19208 22.5008 8.79688 22.501H19.2051C19.8097 22.5006 20.2676 22.0223 20.2676 21.4727V8.48633H7.73438V21.4727ZM11.6875 4.5C11.4024 4.50011 11.2036 4.72306 11.2031 4.95801V5.98633H16.7979V4.95801C16.7974 4.72306 16.5986 4.50012 16.3135 4.5H11.6875Z"),
            )
        }.build()
        return _ic_trash_bin_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTrashBin28Preview() {
    Icon(
        imageVector = Icons.ic_trash_bin_28,
        contentDescription = null,
    )
}