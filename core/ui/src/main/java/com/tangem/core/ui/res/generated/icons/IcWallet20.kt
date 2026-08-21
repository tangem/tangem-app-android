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

private var _ic_wallet_20: ImageVector? = null

val Icons.ic_wallet_20: ImageVector
    get() {
        if (_ic_wallet_20 != null) return _ic_wallet_20!!
        _ic_wallet_20 = ImageVector.Builder(
            name = "ic_wallet_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.1934 4.17582C11.1054 3.56814 12.3334 3.77864 12.9912 4.65532L14.375 6.50004C15.4933 6.7018 16.3417 7.67911 16.3418 8.85551V9.63871C16.7368 9.88816 17 10.3275 17 10.8291V12.8028C16.9999 13.3041 16.7365 13.742 16.3418 13.9913V14.7764C16.3416 16.0988 15.2697 17.1719 13.9473 17.1719H5.39453C4.07219 17.1718 3.00022 16.0987 3 14.7764V8.85551C3.00012 7.5331 4.07212 6.4611 5.39453 6.46098H6.76562L10.1934 4.17582ZM5.39453 7.96098C4.90055 7.9611 4.50012 8.36153 4.5 8.85551V14.7764C4.50022 15.2703 4.90061 15.6718 5.39453 15.6719H13.9473C14.4413 15.6719 14.8416 15.2704 14.8418 14.7764V14.21H13.9473C12.6248 14.2099 11.5527 13.138 11.5527 11.8155C11.5529 10.5189 12.5835 9.46672 13.8701 9.42582C13.8955 9.42322 13.9212 9.42095 13.9473 9.42094H14.8418V8.85551C14.8417 8.36146 14.4413 7.96098 13.9473 7.96098H5.39453ZM13.9473 10.9209C13.4533 10.9211 13.0529 11.3215 13.0527 11.8155C13.0527 12.3096 13.4532 12.7099 13.9473 12.71H15.5V10.9209H13.9473ZM11.791 5.55571C11.6109 5.31578 11.2751 5.25775 11.0254 5.42387L9.46973 6.46098H12.4707L11.791 5.55571Z"),
            )
        }.build()
        return _ic_wallet_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWallet20Preview() {
    Icon(
        imageVector = Icons.ic_wallet_20,
        contentDescription = null,
    )
}