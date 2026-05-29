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

private var _ic_warning_28: ImageVector? = null

val Icons.ic_warning_28: ImageVector
    get() {
        if (_ic_warning_28 != null) return _ic_warning_28!!
        _ic_warning_28 = ImageVector.Builder(
            name = "ic_warning_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.0007 17C14.8057 17.0003 15.4586 17.6529 15.4587 18.458C15.4587 19.2632 14.8058 19.9166 14.0007 19.917C13.1953 19.917 12.5418 19.2634 12.5417 18.458C12.5419 17.6527 13.1954 17 14.0007 17Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.0017 8.9863C14.6914 8.98677 15.2513 9.54652 15.2517 10.2363V14.248C15.2514 14.9378 14.6915 15.4975 14.0017 15.498C13.3115 15.498 12.752 14.9381 12.7517 14.248V10.2363C12.752 9.54623 13.3115 8.9863 14.0017 8.9863Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.8757 4.16696C12.4036 1.87027 15.8683 1.94686 17.2673 4.39645L25.4997 18.8047L25.6247 19.042C26.8174 21.5046 25.0311 24.4334 22.2331 24.4336H5.76828C2.87959 24.4333 1.06801 21.3127 2.50168 18.8047L10.7341 4.39645L10.8757 4.16696ZM15.0964 5.63669C14.6422 4.84152 13.5398 4.79149 13.0046 5.48727L12.905 5.63669L4.67258 20.0449C4.19197 20.8859 4.79883 21.9333 5.76828 21.9336H22.2331C23.1404 21.9334 23.7321 21.0135 23.407 20.205L23.3288 20.0449L15.0964 5.63669Z"),
            )
        }.build()
        return _ic_warning_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWarning28Preview() {
    Icon(
        imageVector = Icons.ic_warning_28,
        contentDescription = null,
    )
}