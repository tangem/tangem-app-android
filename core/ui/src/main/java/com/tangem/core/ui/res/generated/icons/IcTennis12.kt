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

private var _ic_tennis_12: ImageVector? = null

val Icons.ic_tennis_12: ImageVector
    get() {
        if (_ic_tennis_12 != null) return _ic_tennis_12!!
        _ic_tennis_12 = ImageVector.Builder(
            name = "ic_tennis_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.55176 8.10449C10.3511 8.10449 10.9998 8.75242 11 9.55176C11 10.3512 10.3512 11 9.55176 11C8.75242 10.9998 8.10449 10.3511 8.10449 9.55176C8.10467 8.75253 8.75253 8.10467 9.55176 8.10449ZM9.55176 9.10449C9.30482 9.10467 9.10467 9.30482 9.10449 9.55176C9.10449 9.79885 9.30471 9.99982 9.55176 10C9.79896 10 10 9.79896 10 9.55176C9.99982 9.30471 9.79885 9.10449 9.55176 9.10449Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.875 1C7.8434 1 8.77223 1.38464 9.45703 2.06934C10.1418 2.75409 10.5263 3.68296 10.5264 4.65137C10.5264 6.66802 8.89169 8.30273 6.875 8.30273H6.25488C5.38754 8.30286 4.55575 8.64753 3.94238 9.26074L2.96484 10.2373C2.78014 10.422 2.52975 10.5264 2.26855 10.5264C2.00748 10.5263 1.75689 10.4219 1.57227 10.2373L1.28809 9.9541C1.12644 9.79245 1.02722 9.58002 1.00488 9.35449L1 9.25781L1.00488 9.16016C1.02725 8.93466 1.12645 8.72218 1.28809 8.56055L2.26562 7.58398C2.87888 6.97062 3.22355 6.1388 3.22363 5.27148V4.65137C3.22365 3.68296 3.60817 2.7541 4.29297 2.06934C4.97776 1.38461 5.90659 1.00002 6.875 1ZM3.26855 7.95996C3.17567 8.0745 3.07788 8.18578 2.97266 8.29102L2.00586 9.25684L2.26758 9.51953L3.23535 8.55371C3.34056 8.44853 3.45093 8.34969 3.56543 8.25684L3.26855 7.95996ZM3.95605 6.75391C3.91354 6.86865 3.86764 6.98208 3.81543 7.09277L4.43262 7.70996C4.54299 7.65789 4.6561 7.61177 4.77051 7.56934L3.95605 6.75391ZM6.875 2C6.1718 2.00002 5.49725 2.27918 5 2.77637C4.50274 3.27359 4.22365 3.94819 4.22363 4.65137V5.27148C4.22362 5.37943 4.21806 5.48685 4.20996 5.59375L5.93262 7.31543C6.03949 7.30733 6.14696 7.30275 6.25488 7.30273H6.875C8.33946 7.30273 9.52636 6.11568 9.52637 4.65137C9.52635 3.9482 9.24725 3.27359 8.75 2.77637C8.25274 2.27922 7.57819 2 6.875 2Z"),
            )
        }.build()
        return _ic_tennis_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTennis12Preview() {
    Icon(
        imageVector = Icons.ic_tennis_12,
        contentDescription = null,
    )
}