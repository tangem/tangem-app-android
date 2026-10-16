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

private var _ic_shop_28: ImageVector? = null

val Icons.ic_shop_28: ImageVector
    get() {
        if (_ic_shop_28 != null) return _ic_shop_28!!
        _ic_shop_28 = ImageVector.Builder(
            name = "ic_shop_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.5127 3C22.0214 3 23.3515 3.98958 23.7852 5.43457L24.8555 9.00293C24.8945 9.13318 24.9239 9.2658 24.9473 9.39941C24.9504 9.41361 24.9525 9.42801 24.9551 9.44238C24.984 9.62144 25 9.80252 25 9.98438V11.833C25 13.2246 24.3673 14.4675 23.375 15.293V21.584C23.3748 23.4707 21.8448 25 19.958 25H8.04199C6.15518 25 4.62525 23.4707 4.625 21.584V15.293C3.6327 14.4675 3 13.2246 3 11.833V9.98438C3.00001 9.80257 3.01504 9.62139 3.04395 9.44238C3.04655 9.42802 3.04867 9.4136 3.05176 9.39941C3.07509 9.26574 3.10544 9.13324 3.14453 9.00293L4.21484 5.43457C4.64852 3.98959 5.97862 3 7.4873 3H20.5127ZM17.25 14.9453C16.4806 15.7486 15.4166 16.2681 14.2314 16.3281L14 16.334L13.7686 16.3281C12.5834 16.2681 11.5194 15.7487 10.75 14.9453C9.98063 15.7485 8.91655 16.2671 7.73145 16.3271L7.5 16.333L7.26855 16.3271C7.22044 16.3247 7.1727 16.3194 7.125 16.3154V21.584C7.12525 22.09 7.53588 22.5 8.04199 22.5H19.958C20.4641 22.5 20.8748 22.09 20.875 21.584V16.3154C20.8273 16.3194 20.7796 16.3247 20.7314 16.3271L20.5 16.333L20.2686 16.3271C19.0834 16.2671 18.0194 15.7485 17.25 14.9453ZM12 11.834C12.0004 12.9381 12.8958 13.8339 14 13.834C15.1043 13.834 15.9996 12.9381 16 11.834V10.917H12V11.834ZM5.5 11.833C5.5 12.9376 6.39543 13.833 7.5 13.833C8.60457 13.833 9.5 12.9376 9.5 11.833V10.917H5.5V11.833ZM18.5 11.833C18.5 12.9376 19.3954 13.833 20.5 13.833C21.6046 13.833 22.5 12.9375 22.5 11.833V10.917H18.5V11.833ZM7.4873 5.5C7.0825 5.5 6.7257 5.76561 6.60938 6.15332L5.93066 8.41699H22.0693L21.3906 6.15332C21.2743 5.7656 20.9175 5.5 20.5127 5.5H7.4873Z"),
            )
        }.build()
        return _ic_shop_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShop28Preview() {
    Icon(
        imageVector = Icons.ic_shop_28,
        contentDescription = null,
    )
}