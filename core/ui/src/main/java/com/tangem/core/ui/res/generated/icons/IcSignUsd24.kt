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

private var _ic_sign_usd_24: ImageVector? = null

val Icons.ic_sign_usd_24: ImageVector
    get() {
        if (_ic_sign_usd_24 != null) return _ic_sign_usd_24!!
        _ic_sign_usd_24 = ImageVector.Builder(
            name = "ic_sign_usd_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.4499 1.99707C13.0022 1.99707 13.4499 2.44479 13.4499 2.99707V3.9043C14.6485 4.08304 15.7433 4.47992 16.6266 5.05078C17.8461 5.83896 18.776 7.0464 18.7761 8.53027C18.776 9.08239 18.3281 9.53009 17.7761 9.53027C17.2239 9.5302 16.7761 9.08246 16.7761 8.53027C16.776 7.95247 16.4118 7.29344 15.5407 6.73047C14.6797 6.17407 13.4301 5.79688 11.9987 5.79688C10.5674 5.79693 9.31771 6.17405 8.45673 6.73047C7.58581 7.2934 7.22238 7.95253 7.22235 8.53027C7.22237 8.91772 7.29576 9.20552 7.41473 9.42676C7.5313 9.64329 7.72176 9.85205 8.04364 10.042C8.72706 10.4451 9.9496 10.7304 11.9987 10.7305C14.1436 10.7305 15.9374 10.9951 17.2214 11.7285C17.8836 12.1068 18.419 12.6151 18.7819 13.2715C19.1426 13.9239 19.3005 14.6651 19.3005 15.4639C19.3003 17.24 18.3207 18.4782 16.9206 19.2148C15.9388 19.7314 14.735 20.0154 13.4499 20.1318V20.9971C13.4497 21.5492 13.0021 21.9971 12.4499 21.9971C11.8978 21.997 11.4501 21.5491 11.4499 20.9971V20.1816C9.77177 20.1032 8.22372 19.6693 7.0329 18.9639C5.73531 18.1951 4.69718 16.9901 4.69696 15.4639C4.697 14.9117 5.14476 14.4639 5.69696 14.4639C6.24922 14.4639 6.69692 14.9116 6.69696 15.4639C6.69718 15.9991 7.06975 16.661 8.05243 17.2432C9.01349 17.8125 10.4066 18.1962 11.9987 18.1963C13.663 18.1963 15.0538 17.9378 15.9899 17.4453C16.8702 16.9821 17.3003 16.3537 17.3005 15.4639C17.3005 14.9296 17.1963 14.5367 17.0319 14.2393C16.8697 13.9458 16.6172 13.6865 16.2292 13.4648C15.4125 12.9985 14.0548 12.7305 11.9987 12.7305C9.8473 12.7304 8.18164 12.4458 7.02704 11.7646C6.43029 11.4125 5.96312 10.9505 5.65302 10.374C5.3454 9.80208 5.22237 9.17353 5.22235 8.53027C5.22238 7.04632 6.15219 5.83896 7.37177 5.05078C8.47249 4.33948 9.90093 3.89752 11.4499 3.8125V2.99707C11.4499 2.44483 11.8977 1.99714 12.4499 1.99707Z"),
            )
        }.build()
        return _ic_sign_usd_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignUsd24Preview() {
    Icon(
        imageVector = Icons.ic_sign_usd_24,
        contentDescription = null,
    )
}