@file:Suppress("all")

package com.tangem.core.ui.res.generated.assets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.generated.icons.Icons

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _il_account_home: ImageVector? = null

val Icons.il_account_home: ImageVector
    get() {
        if (_il_account_home != null) return _il_account_home!!
        _il_account_home = ImageVector.Builder(
            name = "il_account_home",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23.6676 15.0759C23.9117 14.9575 24.2059 14.9778 24.4336 15.1355L33.6773 21.5514C34.0194 21.7889 34.1027 22.2568 33.8634 22.596C33.7262 22.7904 33.5126 22.9006 33.2909 22.9146L32.1513 23.0914V32.2502C32.1509 32.664 31.8125 33 31.3952 33H16.6046C16.1874 32.9998 15.8488 32.6639 15.8485 32.2502V23.0914L14.7127 22.9146C14.4897 22.9018 14.2745 22.7916 14.1364 22.596C13.8974 22.2569 13.9806 21.7888 14.3225 21.5514L23.5662 15.1355L23.6676 15.0759ZM21.9831 26.75V31.5005H26.0177V26.75C26.0177 26.1513 25.5286 25.6665 24.9249 25.6664H23.0759C22.472 25.6664 21.9831 26.1512 21.9831 26.75Z"),
            )
        }.build()
        return _il_account_home!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountHomePreview() {
    Icon(
        imageVector = Icons.il_account_home,
        contentDescription = null,
    )
}