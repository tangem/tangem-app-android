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

private var _il_account_star: ImageVector? = null

val Icons.il_account_star: ImageVector
    get() {
        if (_il_account_star != null) return _il_account_star!!
        _il_account_star = ImageVector.Builder(
            name = "il_account_star",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.5 14C25.3745 14.0002 26.1736 14.4958 26.5631 15.2793L28.5692 19.2864L33.0144 19.9231C33.8896 20.046 34.6188 20.6595 34.8895 21.5017C35.1599 22.3433 34.9249 23.2645 34.2864 23.8751L34.2874 23.8762L31.0933 26.9393L31.8523 31.2928C32.0048 32.1577 31.6529 33.0346 30.9453 33.5541C30.2378 34.0734 29.2974 34.1444 28.5192 33.7393L24.499 31.6612L20.4809 33.7393L20.4799 33.7383C19.7019 34.1431 18.7621 34.0721 18.0548 33.5531C17.3463 33.0331 16.9941 32.1546 17.1477 31.2888L17.9058 26.9393L14.7126 23.8762L14.7136 23.8751C14.0749 23.2645 13.8401 22.3434 14.1106 21.5017C14.3644 20.7123 15.0197 20.1239 15.8217 19.9521L15.9847 19.9231L20.4279 19.2864L22.436 15.2793C22.8255 14.4956 23.6254 14 24.5 14Z"),
            )
        }.build()
        return _il_account_star!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountStarPreview() {
    Icon(
        imageVector = Icons.il_account_star,
        contentDescription = null,
    )
}