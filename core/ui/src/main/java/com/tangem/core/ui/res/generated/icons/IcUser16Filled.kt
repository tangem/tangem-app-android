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

private var _ic_user_16_filled: ImageVector? = null

val Icons.ic_user_16_filled: ImageVector
    get() {
        if (_ic_user_16_filled != null) return _ic_user_16_filled!!
        _ic_user_16_filled = ImageVector.Builder(
            name = "ic_user_16_filled",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.89307 3.81989C7.07993 2.72335 8.89846 2.72355 10.0854 3.81989C10.0882 3.82242 10.0906 3.82607 10.0933 3.82868C10.095 3.83038 10.0973 3.83185 10.0991 3.83356C11.2883 5.00103 11.3086 6.89092 10.0854 8.02106C8.92409 9.09402 7.02613 9.20506 5.86572 7.99469C4.76758 6.84886 4.65298 4.96573 5.89307 3.81989Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.42139 9.34528C11.1582 9.34536 12.4984 10.7177 12.4985 12.3775C12.4983 12.7224 12.2185 13.0024 11.8735 13.0025H4.12646C3.78142 13.0025 3.50168 12.7225 3.50146 12.3775C3.50163 10.6447 4.9163 9.34542 6.57861 9.34528H9.42139Z"),
            )
        }.build()
        return _ic_user_16_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser16FilledPreview() {
    Icon(
        imageVector = Icons.ic_user_16_filled,
        contentDescription = null,
    )
}