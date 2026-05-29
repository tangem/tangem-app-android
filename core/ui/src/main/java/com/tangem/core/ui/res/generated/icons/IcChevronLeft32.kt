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

private var _ic_chevron_left_32: ImageVector? = null

val Icons.ic_chevron_left_32: ImageVector
    get() {
        if (_ic_chevron_left_32 != null) return _ic_chevron_left_32!!
        _ic_chevron_left_32 = ImageVector.Builder(
            name = "ic_chevron_left_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.7741 6.44214C18.3599 5.85636 19.3094 5.85636 19.8952 6.44214C20.4803 7.02798 20.4808 7.97768 19.8952 8.56324L12.4568 16.0017L19.8952 23.4412C20.4803 24.027 20.4806 24.9767 19.8952 25.5623C19.3096 26.1475 18.3599 26.1473 17.7741 25.5623L9.27414 17.0632C8.68867 16.4777 8.68916 15.528 9.27414 14.9421L17.7741 6.44214Z"),
            )
        }.build()
        return _ic_chevron_left_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronLeft32Preview() {
    Icon(
        imageVector = Icons.ic_chevron_left_32,
        contentDescription = null,
    )
}