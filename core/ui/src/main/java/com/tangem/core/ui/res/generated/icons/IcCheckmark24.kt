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

private var _ic_checkmark_24: ImageVector? = null

val Icons.ic_checkmark_24: ImageVector
    get() {
        if (_ic_checkmark_24 != null) return _ic_checkmark_24!!
        _ic_checkmark_24 = ImageVector.Builder(
            name = "ic_checkmark_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.2784 6.30768C18.6608 5.90939 19.294 5.89615 19.6924 6.27839C20.0907 6.66081 20.104 7.29407 19.7217 7.69245L10.1202 17.6924C9.93164 17.8888 9.67073 18 9.3985 18.0001C9.12637 18 8.86632 17.8887 8.6778 17.6924L4.27838 13.1124C3.89612 12.714 3.90943 12.0808 4.30768 11.6983C4.70604 11.3161 5.33929 11.3294 5.72174 11.7276L9.39752 15.5557L18.2784 6.30768Z"),
            )
        }.build()
        return _ic_checkmark_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCheckmark24Preview() {
    Icon(
        imageVector = Icons.ic_checkmark_24,
        contentDescription = null,
    )
}