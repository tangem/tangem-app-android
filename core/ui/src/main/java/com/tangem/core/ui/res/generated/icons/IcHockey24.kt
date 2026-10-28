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

private var _ic_hockey_24: ImageVector? = null

val Icons.ic_hockey_24: ImageVector
    get() {
        if (_ic_hockey_24 != null) return _ic_hockey_24!!
        _ic_hockey_24 = ImageVector.Builder(
            name = "ic_hockey_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.8096 2.40708C13.5819 1.81651 15.5209 1.75934 16.7901 2.97642C16.8078 2.99186 16.8259 3.00737 16.8428 3.02427C16.8626 3.04405 16.8813 3.0647 16.9004 3.08482L20.9746 7.15904C22.2425 8.4269 22.1912 10.3944 21.5928 12.1903C20.9732 14.0498 19.6735 16.0763 17.875 17.8749C16.0765 19.6734 14.0499 20.973 12.1905 21.5926C10.3946 22.1911 8.42706 22.2423 7.15922 20.9745L3.02543 16.8407C1.75762 15.5728 1.80882 13.6053 2.40726 11.8094C3.02691 9.94997 4.32654 7.92335 6.12504 6.12486C7.92353 4.32636 9.95015 3.02672 11.8096 2.40708ZM17.7764 6.78892C17.706 7.21298 17.5988 7.63908 17.46 8.05552C16.8404 9.91498 15.5407 11.9416 13.7422 13.7401C11.9437 15.5386 9.91711 16.8382 8.05765 17.4579C7.63997 17.597 7.21246 17.7039 6.78715 17.7743L8.57328 19.5604C9.01679 20.0039 9.98726 20.2184 11.5577 19.6952C13.0648 19.193 14.8336 18.0863 16.46 16.4598C18.0864 14.8334 19.1931 13.0646 19.6953 11.5575C20.2185 9.98711 20.0041 9.01663 19.5606 8.5731L17.7764 6.78892ZM15.4268 4.43931C14.9832 3.99581 14.0128 3.78136 12.4424 4.30454C10.9353 4.80676 9.16651 5.91346 7.54008 7.5399C5.91364 9.16633 4.80694 10.9351 4.30472 12.4422C3.78518 14.0017 3.99348 14.9691 4.4307 15.4168C4.43381 15.4199 4.43739 15.4226 4.44047 15.4256C4.88408 15.8692 5.85523 16.0838 7.42582 15.5604C8.93284 15.0581 10.7019 13.9523 12.3282 12.326C13.9545 10.6997 15.0603 8.9307 15.5625 7.42368C16.0786 5.87492 15.8763 4.90978 15.4453 4.45786L15.4268 4.43931Z"),
            )
        }.build()
        return _ic_hockey_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHockey24Preview() {
    Icon(
        imageVector = Icons.ic_hockey_24,
        contentDescription = null,
    )
}