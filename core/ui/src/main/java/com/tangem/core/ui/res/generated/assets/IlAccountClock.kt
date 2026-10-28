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

private var _il_account_clock: ImageVector? = null

val Icons.il_account_clock: ImageVector
    get() {
        if (_il_account_clock != null) return _il_account_clock!!
        _il_account_clock = ImageVector.Builder(
            name = "il_account_clock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2ZM13.5 7C13.5 6.44772 13.0523 6 12.5 6C11.9477 6 11.5 6.44772 11.5 7V11.5H8.5C7.94772 11.5 7.5 11.9477 7.5 12.5C7.5 13.0523 7.94772 13.5 8.5 13.5H12.5C13.0523 13.5 13.5 13.0523 13.5 12.5V7Z"),
            )
        }.build()
        return _il_account_clock!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountClockPreview() {
    Icon(
        imageVector = Icons.il_account_clock,
        contentDescription = null,
    )
}