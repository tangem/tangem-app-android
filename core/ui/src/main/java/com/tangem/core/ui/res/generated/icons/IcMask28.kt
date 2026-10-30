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

private var _ic_mask_28: ImageVector? = null

val Icons.ic_mask_28: ImageVector
    get() {
        if (_ic_mask_28 != null) return _ic_mask_28!!
        _ic_mask_28 = ImageVector.Builder(
            name = "ic_mask_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M22.5 6.15582C22.5023 5.79739 22.333 5.45437 22.0362 5.23394C21.7385 5.01292 21.3495 4.94311 20.9893 5.04937L20.9873 5.05035C16.4275 6.39023 11.5726 6.39023 7.01273 5.05035L7.01077 5.04937C6.65055 4.9431 6.26159 5.01292 5.9639 5.23394C5.6671 5.45437 5.49773 5.79738 5.50003 6.15582V14.6002C5.50008 19.2275 9.29357 23.0005 14 23.0005C18.7065 23.0005 22.5 19.2275 22.5 14.6002V6.15582ZM16.1895 16.1031C16.6917 15.6297 17.4825 15.6529 17.9561 16.1548C18.4296 16.6572 18.4067 17.4489 17.9043 17.9224C15.7166 19.9846 12.2835 19.9846 10.0957 17.9224C9.59339 17.4489 9.57044 16.6572 10.044 16.1548C10.5176 15.6529 11.3084 15.6297 11.8106 16.1031C13.0353 17.2576 14.9648 17.2576 16.1895 16.1031ZM11.5625 9.73101C12.2529 9.73102 12.8125 10.2907 12.8125 10.981C12.8125 11.6714 12.2529 12.231 11.5625 12.231H9.12503C8.43468 12.231 7.87503 11.6714 7.87503 10.981C7.87508 10.2907 8.43472 9.73102 9.12503 9.73101H11.5625ZM18.875 9.73101C19.5653 9.73104 20.125 10.2907 20.125 10.981C20.125 11.6714 19.5654 12.231 18.875 12.231H16.4375C15.7472 12.231 15.1875 11.6714 15.1875 10.981C15.1876 10.2907 15.7472 9.73101 16.4375 9.73101H18.875ZM25 14.6002C25 20.6323 20.063 25.5005 14 25.5005C7.93708 25.5005 3.00008 20.6323 3.00003 14.6002V6.16363C2.99513 5.00509 3.54417 3.91624 4.47367 3.22613C5.4037 2.53571 6.60629 2.32467 7.71683 2.65191C11.8166 3.85658 16.1825 3.85559 20.2823 2.65094C21.3931 2.3233 22.5961 2.53639 23.5264 3.22711C24.4557 3.91722 25.0049 5.00523 25 6.16363V14.6002Z"),
            )
        }.build()
        return _ic_mask_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMask28Preview() {
    Icon(
        imageVector = Icons.ic_mask_28,
        contentDescription = null,
    )
}