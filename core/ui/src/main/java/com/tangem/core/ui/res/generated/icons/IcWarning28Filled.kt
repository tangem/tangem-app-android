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

private var _ic_warning_28_filled: ImageVector? = null

val Icons.ic_warning_28_filled: ImageVector
    get() {
        if (_ic_warning_28_filled != null) return _ic_warning_28_filled!!
        _ic_warning_28_filled = ImageVector.Builder(
            name = "ic_warning_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.8757 4.16696C12.4036 1.87027 15.8683 1.94686 17.2673 4.39645L25.4997 18.8047L25.6247 19.042C26.8174 21.5046 25.0311 24.4334 22.2331 24.4336H5.76828C2.87959 24.4333 1.06801 21.3127 2.50168 18.8047L10.7341 4.39645L10.8757 4.16696ZM14.0017 8.9863C13.3115 8.9863 12.752 9.54623 12.7517 10.2363V14.248C12.752 14.9381 13.3115 15.498 14.0017 15.498C14.6915 15.4975 15.2514 14.9378 15.2517 14.248V10.2363C15.2513 9.54652 14.6914 8.98677 14.0017 8.9863ZM15.4587 18.458C15.4586 17.6529 14.8057 17.0003 14.0007 17C13.1954 17 12.5419 17.6527 12.5417 18.458C12.5418 19.2634 13.1953 19.917 14.0007 19.917C14.8058 19.9166 15.4587 19.2632 15.4587 18.458Z"),
            )
        }.build()
        return _ic_warning_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWarning28FilledPreview() {
    Icon(
        imageVector = Icons.ic_warning_28_filled,
        contentDescription = null,
    )
}