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

private var _ic_atm_16: ImageVector? = null

val Icons.ic_atm_16: ImageVector
    get() {
        if (_ic_atm_16 != null) return _ic_atm_16!!
        _ic_atm_16 = ImageVector.Builder(
            name = "ic_atm_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.72852 9.25903C9.07364 9.25909 9.35352 9.53889 9.35352 9.88403V10.8313C9.35327 11.1762 9.07349 11.4562 8.72852 11.4563C8.38354 11.4562 8.10376 11.1762 8.10352 10.8313V9.88403C8.10352 9.5389 8.38339 9.2591 8.72852 9.25903Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.4023 3.00024C12.3178 3.00024 12.9999 3.7748 13 4.65454V6.71313C13 7.59293 12.3179 8.36743 11.4023 8.36743H11.0557V11.3459C11.0554 12.2255 10.3732 13.0001 9.45801 13.0002H6.54199C5.62665 13.0002 4.94459 12.2255 4.94434 11.3459V8.36743H4.59766C3.68215 8.36743 3 7.59293 3 6.71313V4.65454C3.00006 3.7748 3.68219 3.00024 4.59766 3.00024H11.4023ZM7.65332 11.7502H9.45801C9.61666 11.7501 9.80543 11.6032 9.80566 11.3459V6.30884H7.65332V11.7502ZM6.19434 11.3459C6.1945 11.5291 6.291 11.6541 6.40332 11.7122V6.30884H6.19434V11.3459ZM4.59766 4.25024C4.43887 4.25024 4.25005 4.39691 4.25 4.65454V6.71313C4.25 6.97084 4.43885 7.11743 4.59766 7.11743H4.94434V5.68384C4.94434 5.33866 5.22416 5.05884 5.56934 5.05884H10.4307C10.7756 5.05907 11.0557 5.33881 11.0557 5.68384V7.11743H11.4023C11.5611 7.11743 11.75 6.97084 11.75 6.71313V4.65454C11.7499 4.39691 11.5611 4.25024 11.4023 4.25024H4.59766Z"),
            )
        }.build()
        return _ic_atm_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtm16Preview() {
    Icon(
        imageVector = Icons.ic_atm_16,
        contentDescription = null,
    )
}