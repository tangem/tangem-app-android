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

private var _ic_gift_16: ImageVector? = null

val Icons.ic_gift_16: ImageVector
    get() {
        if (_ic_gift_16 != null) return _ic_gift_16!!
        _ic_gift_16 = ImageVector.Builder(
            name = "ic_gift_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.05859 2.54401C8.78024 1.81385 9.94422 1.81879 10.6582 2.56158C11.2957 3.22493 11.3557 4.25178 10.8428 4.98639H12.7773C13.4522 4.98639 14 5.53421 14 6.20904V7.40338C13.9996 8.06839 13.4672 8.60805 12.8057 8.62311V12.7784C12.8055 13.453 12.2578 14.0001 11.583 14.0001H4.41699C3.74225 14.0001 3.19454 13.453 3.19434 12.7784V8.62311C2.53279 8.60805 2.0004 8.06839 2 7.40338V6.20904C2 5.53421 2.54779 4.98639 3.22266 4.98639H5.15723C4.6441 4.25177 4.70507 3.22501 5.34277 2.56158C6.06232 1.81367 7.2389 1.81259 7.95898 2.55963L7.95996 2.56061C7.97349 2.57469 7.98607 2.59013 7.99902 2.60455C8.01209 2.59 8.02541 2.57481 8.03906 2.56061C8.04501 2.55444 8.05246 2.54988 8.05859 2.54401ZM4.44434 8.62506V12.7501H11.5557V8.62506H4.44434ZM3.25 7.37506H12.75V6.23639H3.25V7.37506ZM7.05957 3.42779C6.83152 3.19074 6.47238 3.19109 6.24414 3.42779C6.00249 3.67919 6.00249 4.0973 6.24414 4.34869L6.33594 4.43658C6.6305 4.69522 7.02177 4.84228 7.37109 4.91803C7.37395 4.81954 7.37537 4.71365 7.37207 4.60455C7.36445 4.35353 7.33726 4.10076 7.2793 3.8819C7.22015 3.65891 7.14034 3.51207 7.05957 3.42779ZM9.75684 3.42779C9.52865 3.19051 9.16868 3.19063 8.94043 3.42779L8.87988 3.50299C8.82059 3.59013 8.76513 3.71447 8.7207 3.8819C8.66273 4.10075 8.63458 4.35353 8.62695 4.60455C8.62366 4.7136 8.62487 4.81958 8.62793 4.91803C8.97751 4.84236 9.36923 4.69551 9.66406 4.43658L9.75684 4.34869C9.99798 4.09728 9.99821 3.67904 9.75684 3.42779Z"),
            )
        }.build()
        return _ic_gift_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGift16Preview() {
    Icon(
        imageVector = Icons.ic_gift_16,
        contentDescription = null,
    )
}