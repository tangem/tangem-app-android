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

private var _ic_sign_usd_20: ImageVector? = null

val Icons.ic_sign_usd_20: ImageVector
    get() {
        if (_ic_sign_usd_20 != null) return _ic_sign_usd_20!!
        _ic_sign_usd_20 = ImageVector.Builder(
            name = "ic_sign_usd_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.3625 2.00439C10.7765 2.00459 11.1125 2.34033 11.1125 2.75439V3.53369C12.0883 3.67315 12.9785 3.99312 13.6936 4.45557C14.6656 5.08428 15.3985 6.04164 15.3987 7.21143C15.3985 7.62544 15.0627 7.96129 14.6487 7.96143C14.2346 7.96133 13.8988 7.62546 13.8987 7.21143C13.8985 6.72063 13.5896 6.17484 12.8791 5.71533C12.1761 5.26063 11.1601 4.95463 9.99924 4.95459C8.83836 4.95462 7.82143 5.26061 7.11838 5.71533C6.40835 6.17476 6.09995 6.72077 6.09982 7.21143C6.09985 7.53068 6.15993 7.77248 6.26096 7.96045C6.36011 8.14481 6.52096 8.31966 6.78732 8.47705C7.35018 8.80943 8.34699 9.03953 9.99924 9.03955C11.723 9.03957 13.1544 9.253 14.175 9.83643C14.7005 10.1369 15.1239 10.5394 15.4104 11.0581C15.6951 11.5737 15.8215 12.1605 15.8215 12.7964C15.8214 14.2022 15.0476 15.1831 13.9348 15.769C13.1401 16.1874 12.1604 16.415 11.1125 16.5054V17.2534C11.1124 17.6674 10.7765 18.0032 10.3625 18.0034C9.9484 18.0034 9.61267 17.6675 9.61252 17.2534V16.5435C8.24926 16.4874 6.99244 16.1381 6.03049 15.5679C4.99455 14.9537 4.17709 13.9981 4.17697 12.7964C4.177 12.3823 4.51286 12.0465 4.92697 12.0464C5.34117 12.0464 5.67694 12.3822 5.67697 12.7964C5.67709 13.2553 5.9956 13.8038 6.79514 14.2778C7.57844 14.7422 8.70979 15.0532 9.99924 15.0532C11.3426 15.0532 12.472 14.8444 13.2365 14.4419C13.9591 14.0613 14.3214 13.5381 14.3215 12.7964C14.3215 12.3585 14.2357 12.0323 14.0979 11.7827C13.9617 11.5362 13.7506 11.321 13.4309 11.1382C12.7609 10.7552 11.6564 10.5396 9.99924 10.5396C8.27032 10.5395 6.94106 10.3102 6.02463 9.76904C5.55195 9.48986 5.18383 9.12465 4.93967 8.67041C4.69734 8.21951 4.59986 7.72236 4.59982 7.21143C4.59995 6.04178 5.33204 5.08428 6.30392 4.45557C7.19314 3.88043 8.35378 3.52514 9.61252 3.46436V2.75439C9.61255 2.34021 9.94833 2.00439 10.3625 2.00439Z"),
            )
        }.build()
        return _ic_sign_usd_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignUsd20Preview() {
    Icon(
        imageVector = Icons.ic_sign_usd_20,
        contentDescription = null,
    )
}