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

private var _ic_tennis_16: ImageVector? = null

val Icons.ic_tennis_16: ImageVector
    get() {
        if (_ic_tennis_16 != null) return _ic_tennis_16!!
        _ic_tennis_16 = ImageVector.Builder(
            name = "ic_tennis_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.2432 10.4873C13.2132 10.4874 14 11.274 14 12.2441C13.9997 13.214 13.2131 13.9999 12.2432 14C11.2733 13.9999 10.4866 13.214 10.4863 12.2441C10.4863 11.2741 11.2731 10.4874 12.2432 10.4873ZM12.2432 11.7373C11.9634 11.7374 11.7363 11.9645 11.7363 12.2441C11.7366 12.5236 11.9636 12.7499 12.2432 12.75C12.5227 12.7499 12.7497 12.5236 12.75 12.2441C12.75 11.9644 12.5229 11.7374 12.2432 11.7373Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.04492 2C10.209 2 11.3262 2.46204 12.1494 3.28516C12.9726 4.10831 13.4346 5.22553 13.4346 6.38965C13.4344 8.81364 11.469 10.7783 9.04492 10.7783H8.30469C7.27585 10.7784 6.28902 11.1876 5.56152 11.915L4.39453 13.082C4.16874 13.3076 3.86215 13.4346 3.54297 13.4346C3.22397 13.4345 2.91806 13.3075 2.69238 13.082L2.35254 12.7422C2.12683 12.5164 2 12.2099 2 11.8906C2.00009 11.5714 2.12679 11.2648 2.35254 11.0391L3.51953 9.87305L3.65234 9.7334C4.29665 9.02183 4.65616 8.09433 4.65625 7.12988V6.38965C4.65625 5.22553 5.11822 4.10831 5.94141 3.28516C6.76449 2.4622 7.88099 2.0001 9.04492 2ZM4.74023 10.3848C4.63443 10.5136 4.52222 10.6379 4.40332 10.7568L3.26855 11.8906L3.54297 12.165L4.67773 11.0312C4.79678 10.9122 4.9208 10.7993 5.0498 10.6934L4.74023 10.3848ZM5.56934 8.95117C5.52466 9.06866 5.47701 9.18492 5.42383 9.29883L6.13477 10.0107C6.24876 9.95749 6.36484 9.90898 6.48242 9.86426L5.56934 8.95117ZM9.04492 3.25C8.21241 3.2501 7.41387 3.58128 6.8252 4.16992C6.23662 4.75863 5.90625 5.55721 5.90625 6.38965V7.12988C5.90624 7.25482 5.89967 7.37915 5.89062 7.50293L7.92969 9.54297C8.05408 9.53384 8.17912 9.52833 8.30469 9.52832H9.04492C10.7787 9.52832 12.1844 8.12324 12.1846 6.38965C12.1846 5.55726 11.8541 4.75862 11.2656 4.16992C10.6769 3.58119 9.87756 3.25 9.04492 3.25Z"),
            )
        }.build()
        return _ic_tennis_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTennis16Preview() {
    Icon(
        imageVector = Icons.ic_tennis_16,
        contentDescription = null,
    )
}