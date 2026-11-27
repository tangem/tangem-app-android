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

private var _il_account_airplane: ImageVector? = null

val Icons.il_account_airplane: ImageVector
    get() {
        if (_il_account_airplane != null) return _il_account_airplane!!
        _il_account_airplane = ImageVector.Builder(
            name = "il_account_airplane",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24 15C25.3225 15.0001 26.4014 16.079 26.4016 17.4016V20.8717L32.332 24.7737L32.3359 24.7767C32.8495 25.1194 32.9999 25.6411 33 26.0617V27.4642C32.9999 27.9079 32.7675 28.2612 32.4564 28.4593C32.1652 28.6446 31.7859 28.7081 31.4329 28.5906C31.4114 28.5834 31.39 28.5752 31.3692 28.5661L26.4016 26.3878V28.858L27.6337 29.9539L27.666 29.9843C27.7566 30.0749 28.0519 30.3509 28.0519 30.8462V32.2478C28.0519 32.4793 27.9445 32.6988 27.762 32.8413C27.5795 32.9836 27.3411 33.0335 27.1166 32.9774L24 32.1978L20.8834 32.9774C20.6589 33.0336 20.4205 32.9837 20.238 32.8413C20.0555 32.6988 19.9491 32.4793 19.9491 32.2478V30.7639C19.9491 30.5406 20.0368 30.3475 20.0793 30.2625C20.1336 30.154 20.2184 30.0176 20.334 29.902L20.3663 29.8717L21.5984 28.7757V26.3056L16.6543 28.4721C15.9078 28.8807 15.0001 28.3419 15 27.4642V26.0617C15.0002 25.5143 15.3367 25.0844 15.5504 24.8707L15.6072 24.8188C15.6269 24.8028 15.6477 24.7877 15.669 24.7737L21.5984 20.8727V17.4016C21.5986 16.079 22.6774 15.0001 24 15Z"),
            )
        }.build()
        return _il_account_airplane!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountAirplanePreview() {
    Icon(
        imageVector = Icons.il_account_airplane,
        contentDescription = null,
    )
}