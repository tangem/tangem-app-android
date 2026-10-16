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

private var _ic_search_20: ImageVector? = null

val Icons.ic_search_20: ImageVector
    get() {
        if (_ic_search_20 != null) return _ic_search_20!!
        _ic_search_20 = ImageVector.Builder(
            name = "ic_search_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.24707 2.99707C12.6988 2.99707 15.497 5.79539 15.4971 9.24707C15.4971 10.7026 14.9974 12.0402 14.1631 13.1025L16.7803 15.7197C17.0732 16.0126 17.0732 16.4874 16.7803 16.7803C16.4874 17.0732 16.0126 17.0732 15.7197 16.7803L13.1025 14.1631C12.0402 14.9974 10.7026 15.4971 9.24707 15.4971C5.7954 15.4969 2.99707 12.6988 2.99707 9.24707C2.99719 5.79547 5.79547 2.99719 9.24707 2.99707ZM9.24707 4.49707C6.6239 4.49719 4.49719 6.6239 4.49707 9.24707C4.49707 11.8703 6.62382 13.9969 9.24707 13.9971C11.8704 13.9971 13.9971 11.8704 13.9971 9.24707C13.997 6.62382 11.8703 4.49707 9.24707 4.49707Z"),
            )
        }.build()
        return _ic_search_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSearch20Preview() {
    Icon(
        imageVector = Icons.ic_search_20,
        contentDescription = null,
    )
}