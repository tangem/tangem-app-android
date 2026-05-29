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
                pathData = addPathNodes("M12.4499 1.99683C13.0022 1.99683 13.4499 2.44454 13.4499 2.99683V3.90405C14.6485 4.0828 15.7433 4.47968 16.6266 5.05054C17.8461 5.83872 18.776 7.04615 18.7761 8.53003C18.776 9.08214 18.3281 9.52984 17.7761 9.53003C17.2239 9.52995 16.7761 9.08221 16.7761 8.53003C16.776 7.95222 16.4118 7.2932 15.5407 6.73022C14.6797 6.17383 13.4301 5.79663 11.9987 5.79663C10.5674 5.79669 9.31771 6.17381 8.45673 6.73022C7.58581 7.29315 7.22238 7.95229 7.22235 8.53003C7.22237 8.91748 7.29576 9.20527 7.41473 9.42651C7.5313 9.64305 7.72176 9.8518 8.04364 10.0417C8.72706 10.4449 9.9496 10.7302 11.9987 10.7302C14.1436 10.7302 15.9374 10.9949 17.2214 11.7283C17.8836 12.1066 18.419 12.6148 18.7819 13.2712C19.1426 13.9237 19.3005 14.6648 19.3005 15.4636C19.3003 17.2398 18.3207 18.478 16.9206 19.2146C15.9388 19.7311 14.735 20.0151 13.4499 20.1316V20.9968C13.4497 21.5489 13.0021 21.9968 12.4499 21.9968C11.8978 21.9968 11.4501 21.5489 11.4499 20.9968V20.1814C9.77177 20.103 8.22372 19.6691 7.0329 18.9636C5.73531 18.1949 4.69718 16.9899 4.69696 15.4636C4.697 14.9114 5.14476 14.4637 5.69696 14.4636C6.24922 14.4636 6.69692 14.9114 6.69696 15.4636C6.69718 15.9988 7.06975 16.6607 8.05243 17.2429C9.01349 17.8122 10.4066 18.196 11.9987 18.196C13.663 18.196 15.0538 17.9376 15.9899 17.4451C16.8702 16.9819 17.3003 16.3535 17.3005 15.4636C17.3005 14.9294 17.1963 14.5365 17.0319 14.239C16.8697 13.9455 16.6172 13.6863 16.2292 13.4646C15.4125 12.9982 14.0548 12.7302 11.9987 12.7302C9.8473 12.7302 8.18164 12.4456 7.02704 11.7644C6.43029 11.4123 5.96312 10.9502 5.65302 10.3738C5.3454 9.80183 5.22237 9.17329 5.22235 8.53003C5.22238 7.04608 6.15219 5.83871 7.37177 5.05054C8.47249 4.33923 9.90093 3.89728 11.4499 3.81226V2.99683C11.4499 2.44459 11.8977 1.9969 12.4499 1.99683Z"),
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