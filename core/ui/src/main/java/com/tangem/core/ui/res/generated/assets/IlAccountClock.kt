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
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24 14C29.5228 14 34 18.4772 34 24C34 29.5228 29.5228 34 24 34C18.4772 34 14 29.5228 14 24C14 18.4772 18.4772 14 24 14ZM25.5 19C25.5 18.4477 25.0523 18 24.5 18C23.9477 18 23.5 18.4477 23.5 19V23.5H20.5C19.9477 23.5 19.5 23.9477 19.5 24.5C19.5 25.0523 19.9477 25.5 20.5 25.5H24.5C25.0523 25.5 25.5 25.0523 25.5 24.5V19Z"),
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