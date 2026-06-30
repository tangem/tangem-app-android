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

private var _ic_wallet_16: ImageVector? = null

val Icons.ic_wallet_16: ImageVector
    get() {
        if (_ic_wallet_16 != null) return _ic_wallet_16!!
        _ic_wallet_16 = ImageVector.Builder(
            name = "ic_wallet_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1094 3C13.1045 3.00018 13.999 3.75613 13.999 4.79199V11.209C13.999 12.2448 13.1045 13.0008 12.1094 13.001H3.88965C2.89442 13.001 2.00007 12.2449 2 11.209V4.79199C2 3.75601 2.89438 3 3.88965 3H12.1094ZM3.25 11.209C3.25008 11.4618 3.48817 11.751 3.88965 11.751H12.1094C12.5106 11.7508 12.7489 11.4617 12.749 11.209V10.668H11.4766C10.4816 10.6678 9.58724 9.91158 9.58691 8.87598C9.58691 7.84008 10.4814 7.08411 11.4766 7.08398H12.749V6.58398H3.88965C3.66831 6.58398 3.45263 6.54317 3.25 6.47363V11.209ZM11.4766 8.33398C11.0752 8.3341 10.8369 8.62322 10.8369 8.87598C10.8373 9.12859 11.0755 9.41785 11.4766 9.41797H12.749V8.33398H11.4766ZM3.88965 4.25C3.48811 4.25 3.25 4.53919 3.25 4.79199L3.26074 4.8877C3.31186 5.11231 3.53839 5.33398 3.88965 5.33398H12.749V4.79199C12.749 4.53926 12.5107 4.25017 12.1094 4.25H3.88965Z"),
            )
        }.build()
        return _ic_wallet_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWallet16Preview() {
    Icon(
        imageVector = Icons.ic_wallet_16,
        contentDescription = null,
    )
}