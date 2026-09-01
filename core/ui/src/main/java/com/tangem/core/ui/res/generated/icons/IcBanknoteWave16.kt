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

private var _ic_banknote_wave_16: ImageVector? = null

val Icons.ic_banknote_wave_16: ImageVector
    get() {
        if (_ic_banknote_wave_16 != null) return _ic_banknote_wave_16!!
        _ic_banknote_wave_16 = ImageVector.Builder(
            name = "ic_banknote_wave_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.01367 9.16753C5.35881 9.16753 5.63861 9.44741 5.63867 9.79253C5.63867 10.1377 5.35885 10.4175 5.01367 10.4175H4.41699C4.07181 10.4175 3.79199 10.1377 3.79199 9.79253C3.79205 9.44741 4.07185 9.16753 4.41699 9.16753H5.01367Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M8 5.88335C9.16952 5.88359 10.1182 6.83194 10.1182 8.00152C10.1179 9.17083 9.16933 10.1194 8 10.1197C6.83047 10.1197 5.88214 9.17098 5.88184 8.00152C5.88184 6.83179 6.83028 5.88335 8 5.88335ZM8 7.13335C7.52063 7.13335 7.13184 7.52215 7.13184 8.00152C7.13214 8.48062 7.52082 8.86968 8 8.86968C8.47898 8.86944 8.86786 8.48048 8.86816 8.00152C8.86816 7.52229 8.47917 7.13359 8 7.13335Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.583 5.58452C11.9282 5.58452 12.208 5.86435 12.208 6.20952C12.2078 6.55453 11.9281 6.83452 11.583 6.83452H10.9863C10.6413 6.83452 10.3615 6.55453 10.3613 6.20952C10.3613 5.86435 10.6412 5.58452 10.9863 5.58452H11.583Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M7.98926 3.52105C9.5586 3.12872 11.196 3.08965 12.7822 3.40679L13.0166 3.45366C13.5876 3.56786 13.9986 4.06972 13.999 4.65191V11.1255C13.9989 11.4915 13.8347 11.8378 13.5518 12.0699C13.2686 12.302 12.8962 12.3956 12.5371 12.3238C11.1325 12.043 9.68267 12.077 8.29297 12.4244L8.15137 12.4595C6.48885 12.8751 4.75462 12.9167 3.07422 12.5806L2.98242 12.5621C2.41139 12.4476 2 11.9463 2 11.3638V4.89116C2.00017 4.52521 2.16424 4.17787 2.44727 3.94585L2.55762 3.86577C2.78551 3.71808 3.05658 3.65012 3.32715 3.67339L3.46191 3.69292L3.99023 3.78374C5.22807 3.96069 6.49066 3.89544 7.70703 3.59136L7.98926 3.52105ZM12.5371 4.63238C11.1325 4.35156 9.68266 4.38657 8.29297 4.73394L8.00977 4.80425C6.45117 5.19386 4.82605 5.23452 3.25 4.92437V11.3404L3.31934 11.355C4.81809 11.6547 6.36485 11.6182 7.84766 11.2476L7.98926 11.2115C9.54767 10.8219 11.1731 10.7814 12.749 11.0914V4.67437L12.5371 4.63238Z"),
            )
        }.build()
        return _ic_banknote_wave_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBanknoteWave16Preview() {
    Icon(
        imageVector = Icons.ic_banknote_wave_16,
        contentDescription = null,
    )
}