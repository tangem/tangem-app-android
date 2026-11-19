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
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color.White),
                fillAlpha = 0.5f,
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M22 20C22 18.8954 22.8954 18 24 18H31C32.1046 18 33 18.8954 33 20V31C33 32.1046 32.1046 33 31 33H24C22.8954 33 22 32.1046 22 31V20Z"),
            )
            addPath(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15 17C15 15.8954 15.8954 15 17 15H24C25.1046 15 26 15.8954 26 17V28C26 29.1046 25.1046 30 24 30H17C15.8954 30 15 29.1046 15 28V17Z"),
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