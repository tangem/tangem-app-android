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

private var _ic_loading_spinner_32: ImageVector? = null

val Icons.ic_loading_spinner_32: ImageVector
    get() {
        if (_ic_loading_spinner_32 != null) return _ic_loading_spinner_32!!
        _ic_loading_spinner_32 = ImageVector.Builder(
            name = "ic_loading_spinner_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 2C18.7689 2 21.476 2.82103 23.7783 4.35938C26.0805 5.89771 27.875 8.08449 28.9346 10.6426C29.9941 13.2007 30.2716 16.0158 29.7314 18.7314C29.1912 21.4471 27.8573 23.9415 25.8994 25.8994C23.9415 27.8573 21.4471 29.1912 18.7314 29.7314C16.0158 30.2716 13.2007 29.9941 10.6426 28.9346C8.0845 27.875 5.8977 26.0805 4.35938 23.7783C2.82104 21.476 2 18.7689 2 16C2 15.1716 2.67157 14.5 3.5 14.5C4.32843 14.5 5 15.1716 5 16C5 18.1755 5.64487 20.3024 6.85352 22.1113C8.06216 23.9202 9.78015 25.3305 11.79 26.1631C13.7999 26.9956 16.0119 27.2134 18.1455 26.7891C20.2793 26.3646 22.2399 25.3167 23.7783 23.7783C25.3166 22.24 26.3646 20.2801 26.7891 18.1465C27.2135 16.0127 26.9956 13.8 26.1631 11.79C25.3305 9.78015 23.9202 8.06216 22.1113 6.85352C20.3024 5.64487 18.1756 5 16 5C15.1716 5 14.5 4.32843 14.5 3.5C14.5 2.67157 15.1716 2 16 2Z"),
            )
        }.build()
        return _ic_loading_spinner_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLoadingSpinner32Preview() {
    Icon(
        imageVector = Icons.ic_loading_spinner_32,
        contentDescription = null,
    )
}