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

private var _il_account_gacha: ImageVector? = null

val Icons.il_account_gacha: ImageVector
    get() {
        if (_il_account_gacha != null) return _il_account_gacha!!
        _il_account_gacha = ImageVector.Builder(
            name = "il_account_gacha",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.White),
                fillAlpha = 0.5f,
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 8C10 6.89543 10.8954 6 12 6H19C20.1046 6 21 6.89543 21 8V19C21 20.1046 20.1046 21 19 21H12C10.8954 21 10 20.1046 10 19V8Z"),
            )
            addPath(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3 5C3 3.89543 3.89543 3 5 3H12C13.1046 3 14 3.89543 14 5V16C14 17.1046 13.1046 18 12 18H5C3.89543 18 3 17.1046 3 16V5Z"),
            )
        }.build()
        return _il_account_gacha!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountGachaPreview() {
    Icon(
        imageVector = Icons.il_account_gacha,
        contentDescription = null,
    )
}