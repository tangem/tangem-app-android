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

private var _ic_shop_20: ImageVector? = null

val Icons.ic_shop_20: ImageVector
    get() {
        if (_ic_shop_20 != null) return _ic_shop_20!!
        _ic_shop_20 = ImageVector.Builder(
            name = "ic_shop_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.1748 3C15.1194 3 15.9522 3.6197 16.2236 4.52441L16.9102 6.81152C16.9415 6.91611 16.9636 7.02298 16.9785 7.13086C16.9796 7.13962 16.9807 7.14838 16.9814 7.15723C16.9927 7.24626 17 7.33582 17 7.42578V8.61133C17 9.49524 16.5933 10.2823 15.959 10.8018V14.8613C15.9589 16.0424 15.0004 16.9998 13.8193 17H6.18066C4.99952 16.9999 4.04205 16.0425 4.04199 14.8613V10.8047C3.40653 10.2852 3.00005 9.49617 3 8.61133V7.42578C3.00001 7.33521 3.00621 7.2449 3.01758 7.15527C3.0183 7.14709 3.01953 7.13897 3.02051 7.13086C3.03546 7.02294 3.05846 6.91615 3.08984 6.81152L3.77637 4.52441C4.04778 3.6197 4.88064 3 5.8252 3H14.1748ZM7.91406 10.5273C7.39614 11.0892 6.65732 11.4443 5.83301 11.4443C5.73473 11.4443 5.6377 11.4385 5.54199 11.4287V14.8613C5.54205 15.2141 5.82796 15.4999 6.18066 15.5H13.8193C14.172 15.4998 14.4589 15.214 14.459 14.8613V11.4287L14.4561 11.4297L14.167 11.4443L13.877 11.4297C13.1695 11.3578 12.5394 11.0253 12.083 10.5303C11.6268 11.0252 10.9971 11.3578 10.29 11.4297L10 11.4443L9.71094 11.4297C9.00216 11.3578 8.37061 11.0241 7.91406 10.5273ZM4.5 8.61133C4.50009 9.34754 5.09684 9.94416 5.83301 9.94434C6.56933 9.94434 7.16691 9.34765 7.16699 8.61133V7.97168H4.5V8.61133ZM8.66699 8.61035C8.66699 9.34664 9.26378 9.94416 10 9.94434C10.7329 9.94407 11.3278 9.35186 11.333 8.62012V7.97168H8.66699V8.61035ZM12.834 8.61035C12.834 8.61361 12.833 8.61686 12.833 8.62012C12.8378 9.3524 13.4336 9.94434 14.167 9.94434C14.9032 9.94418 15.4999 9.34756 15.5 8.61133V7.97168H12.834V8.61035ZM5.8252 4.5C5.54312 4.5 5.29402 4.68492 5.21289 4.95508L4.75781 6.47168H15.2422L14.7871 4.95508C14.706 4.68492 14.4569 4.5 14.1748 4.5H5.8252Z"),
            )
        }.build()
        return _ic_shop_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShop20Preview() {
    Icon(
        imageVector = Icons.ic_shop_20,
        contentDescription = null,
    )
}