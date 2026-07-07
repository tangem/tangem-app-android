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

private var _ic_success_20: ImageVector? = null

val Icons.ic_success_20: ImageVector
    get() {
        if (_ic_success_20 != null) return _ic_success_20!!
        _ic_success_20 = ImageVector.Builder(
            name = "ic_success_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.376 7.47168C12.6689 7.17882 13.1436 7.1788 13.4365 7.47168C13.7292 7.76458 13.7293 8.23939 13.4365 8.53223L9.4707 12.4971C9.44261 12.5403 9.40997 12.5822 9.37207 12.6201C9.07922 12.9125 8.60429 12.9126 8.31152 12.6201L6.46973 10.7783C6.17688 10.4855 6.17699 10.0097 6.46973 9.7168C6.76257 9.42419 7.23743 9.42419 7.53027 9.7168L8.83008 11.0166L12.376 7.47168Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 2.02441C14.4047 2.02453 17.9755 5.59526 17.9756 10C17.9755 14.4047 14.4047 17.9755 10 17.9756C5.59526 17.9755 2.02453 14.4047 2.02441 10C2.02453 5.59526 5.59526 2.02453 10 2.02441ZM10 3.52441C6.42369 3.52453 3.52453 6.42369 3.52441 10C3.52453 13.5763 6.42369 16.4755 10 16.4756C13.5763 16.4755 16.4755 13.5763 16.4756 10C16.4755 6.42369 13.5763 3.52453 10 3.52441Z"),
            )
        }.build()
        return _ic_success_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSuccess20Preview() {
    Icon(
        imageVector = Icons.ic_success_20,
        contentDescription = null,
    )
}