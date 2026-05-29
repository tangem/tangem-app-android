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

private var _ic_dots_vertical_12: ImageVector? = null

val Icons.ic_dots_vertical_12: ImageVector
    get() {
        if (_ic_dots_vertical_12 != null) return _ic_dots_vertical_12!!
        _ic_dots_vertical_12 = ImageVector.Builder(
            name = "ic_dots_vertical_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.07519 8.25488C6.45278 8.29316 6.74988 8.61143 6.75 9.00098C6.75 9.41468 6.4137 9.75098 6 9.75098C5.61237 9.75091 5.29258 9.45571 5.2539 9.07812L5.25 9.00098C5.24855 8.58461 5.58827 8.25099 5.99902 8.25098L6.07519 8.25488Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.07519 5.25391C6.45282 5.29219 6.74994 5.6104 6.75 6C6.74993 6.41364 6.41366 6.75 6 6.75C5.61241 6.74993 5.29263 6.45467 5.2539 6.07715L5.25 6C5.24848 5.58358 5.58823 5.25001 5.99902 5.25L6.07519 5.25391Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.07519 2.25293C6.45281 2.29123 6.74994 2.60943 6.75 2.99902C6.7498 3.41256 6.41358 3.74902 6 3.74902C5.61249 3.74896 5.29274 3.45358 5.2539 3.07617L5.25 2.99902C5.24848 2.5826 5.58823 2.24903 5.99902 2.24902L6.07519 2.25293Z"),
            )
        }.build()
        return _ic_dots_vertical_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDotsVertical12Preview() {
    Icon(
        imageVector = Icons.ic_dots_vertical_12,
        contentDescription = null,
    )
}