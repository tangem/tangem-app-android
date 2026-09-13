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

private var _ic_government_20: ImageVector? = null

val Icons.ic_government_20: ImageVector
    get() {
        if (_ic_government_20 != null) return _ic_government_20!!
        _ic_government_20 = ImageVector.Builder(
            name = "ic_government_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.87406 3.01066C9.99941 2.98929 10.1288 3.00004 10.25 3.04289L16.1914 5.14445C16.4908 5.25042 16.6913 5.53393 16.6914 5.85148V7.50285C16.691 7.91672 16.3554 8.25285 15.9414 8.25285H15.7539V11.955C15.9368 11.9872 16.1036 12.086 16.2188 12.2353L16.8428 13.0448C16.944 13.1761 16.9991 13.3382 16.9991 13.5038L16.999 15.6864C16.9986 16.1003 16.663 16.4364 16.249 16.4364H3.75098C3.33701 16.4364 3.00138 16.1003 3.00098 15.6864L3.00102 13.5038C3.00102 13.3382 3.05612 13.1761 3.15727 13.0448L3.78227 12.2353L3.83891 12.1698C3.94961 12.0563 4.0934 11.9808 4.24906 11.954V8.25285H4.05863C3.64467 8.25285 3.30904 7.91672 3.30863 7.50285V5.85148C3.30883 5.53393 3.50925 5.25042 3.80863 5.14445L9.75004 3.04289L9.87406 3.01066ZM4.50102 13.7587L4.50098 14.9364H15.499L15.4991 13.7587L15.2559 13.4433H4.74418L4.50102 13.7587ZM5.74906 11.9433H7.71V8.25285H5.74906V11.9433ZM9.21 11.9433H10.9913V8.25285H9.21V11.9433ZM12.4913 11.9433H14.2539V8.25285H12.4913V11.9433ZM4.80863 6.38176V6.75285H15.1914V6.38176L10 4.54484L4.80863 6.38176Z"),
            )
        }.build()
        return _ic_government_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGovernment20Preview() {
    Icon(
        imageVector = Icons.ic_government_20,
        contentDescription = null,
    )
}