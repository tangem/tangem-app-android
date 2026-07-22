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

private var _ic_bell_cross_28: ImageVector? = null

val Icons.ic_bell_cross_28: ImageVector
    get() {
        if (_ic_bell_cross_28 != null) return _ic_bell_cross_28!!
        _ic_bell_cross_28 = ImageVector.Builder(
            name = "ic_bell_cross_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.57854 3.5791C4.09908 3.05856 4.94276 3.05857 5.4633 3.5791L24.4223 22.5371C24.9421 23.0576 24.9423 23.9015 24.4223 24.4219C23.9019 24.9423 23.0571 24.9419 22.5365 24.4219L20.5317 22.417H18.1742C18.0391 23.3009 17.6258 24.1257 16.9858 24.7656C16.1935 25.5574 15.1186 26.0023 13.9985 26.002C12.8789 26.0023 11.8041 25.558 11.0121 24.7666C10.3718 24.1265 9.95867 23.3013 9.82366 22.417H6.89592C5.91413 22.4169 4.97199 22.0272 4.27776 21.333C3.58354 20.6386 3.19381 19.6967 3.19377 18.7148L3.19866 18.5234C3.24637 17.574 3.6442 16.6732 4.31877 15.9971L4.31975 15.9951L5.58244 14.7334V7.46777L3.57854 5.46387C3.058 4.94333 3.05801 4.09965 3.57854 3.5791ZM12.3989 22.417C12.4847 22.6324 12.6126 22.831 12.7797 22.998C13.1028 23.3209 13.5418 23.5022 13.9985 23.502C14.4553 23.5021 14.8949 23.3201 15.2182 22.9971C15.3851 22.8301 15.5142 22.632 15.6 22.417H12.3989ZM8.08244 15.251C8.08244 15.5825 7.9506 15.9013 7.71623 16.1357L6.08733 17.7637C5.83563 18.0165 5.69408 18.3589 5.69377 18.7158C5.69407 19.0345 5.82107 19.3402 6.04631 19.5654C6.27171 19.7906 6.57743 19.9169 6.89592 19.917H18.0317L8.08244 9.96777V15.251Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.9994 1.99707C18.6481 1.99713 22.4163 5.76612 22.4164 10.415V14.8398C22.4163 15.5301 21.8567 16.0898 21.1664 16.0898C20.4762 16.0898 19.9165 15.5301 19.9164 14.8398V10.415C19.9163 7.14648 17.2671 4.49713 13.9994 4.49707C12.8819 4.49707 11.8401 4.80572 10.9506 5.3418C10.3595 5.69813 9.59126 5.508 9.23479 4.91699C8.87845 4.32583 9.06855 3.55763 9.65959 3.20117C10.9277 2.4368 12.4143 1.99707 13.9994 1.99707Z"),
            )
        }.build()
        return _ic_bell_cross_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBellCross28Preview() {
    Icon(
        imageVector = Icons.ic_bell_cross_28,
        contentDescription = null,
    )
}