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

private var _ic_wallet_24: ImageVector? = null

val Icons.ic_wallet_24: ImageVector
    get() {
        if (_ic_wallet_24 != null) return _ic_wallet_24!!
        _ic_wallet_24 = ImageVector.Builder(
            name = "ic_wallet_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.8809 3C20.6094 3 21.9979 4.40955 21.998 6.13281V11.4482C21.9981 11.4541 21.999 11.46 21.999 11.4658C21.999 11.4714 21.9981 11.4769 21.998 11.4824V15.7148C21.9981 15.7207 21.999 15.7266 21.999 15.7324C21.999 15.738 21.9981 15.7435 21.998 15.749V17.8652C21.9979 19.5885 20.6094 20.999 18.8809 20.999H5.11719C3.38873 20.9989 2.00014 19.5884 2 17.8652V6.13281C2.00018 4.40964 3.38875 3.00015 5.11719 3H18.8809ZM4 17.8652C4.00014 18.4978 4.50718 18.9989 5.11719 18.999H18.8809C19.491 18.999 19.9979 18.4979 19.998 17.8652V16.7324H17.8223C16.0937 16.7323 14.7051 15.3219 14.7051 13.5986C14.7053 11.8755 16.0938 10.466 17.8223 10.4658H19.998V9.2666H5.11719C4.72279 9.26657 4.34655 9.19139 4 9.05762V17.8652ZM17.8223 12.4658C17.2123 12.466 16.7053 12.9661 16.7051 13.5986C16.7051 14.2313 17.2122 14.7323 17.8223 14.7324H19.998V12.4658H17.8223ZM5.11719 5C4.50721 5.00015 4.00018 5.50027 4 6.13281C4.00014 6.76538 4.50718 7.26645 5.11719 7.2666H19.998V6.13281C19.9979 5.50018 19.491 5 18.8809 5H5.11719Z"),
            )
        }.build()
        return _ic_wallet_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWallet24Preview() {
    Icon(
        imageVector = Icons.ic_wallet_24,
        contentDescription = null,
    )
}