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

private var _il_account_user: ImageVector? = null

val Icons.il_account_user: ImageVector
    get() {
        if (_il_account_user != null) return _il_account_user!!
        _il_account_user = ImageVector.Builder(
            name = "il_account_user",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.2749 16.4708C22.3722 14.5097 25.5916 14.5097 27.6889 16.4708C27.6917 16.4734 27.6941 16.4769 27.6967 16.4796C27.701 16.4836 27.7062 16.4871 27.7104 16.4912C29.8009 18.5684 29.8326 21.9174 27.6889 23.9226C25.633 25.8453 22.2749 26.0368 20.232 23.8799C18.2973 21.837 18.1044 18.5007 20.2749 16.4708Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M26.5687 26.609C29.6536 26.6091 31.9997 29.0704 32 32.0062C32 32.555 31.5522 33 30.9999 33H17.0001C16.4479 32.9999 16 32.555 16 32.0062C16.0003 28.9409 18.4779 26.6092 21.4323 26.609H26.5687Z"),
            )
        }.build()
        return _il_account_user!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountUserPreview() {
    Icon(
        imageVector = Icons.il_account_user,
        contentDescription = null,
    )
}