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

private var _ic_warning_16_filled: ImageVector? = null

val Icons.ic_warning_16_filled: ImageVector
    get() {
        if (_ic_warning_16_filled != null) return _ic_warning_16_filled!!
        _ic_warning_16_filled = ImageVector.Builder(
            name = "ic_warning_16_filled",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.36702 3.20146C7.08883 1.93351 8.91183 1.93349 9.63363 3.20146L13.7498 10.427C14.4628 11.6803 13.5637 13.246 12.117 13.2464H3.88363C2.4365 13.2462 1.53656 11.6807 2.25081 10.427L6.36702 3.20146ZM7.99984 9.47587C7.58579 9.47607 7.24984 9.81178 7.24984 10.2259C7.25017 10.6397 7.58599 10.9757 7.99984 10.9759C8.41381 10.9758 8.74951 10.6398 8.74984 10.2259C8.74984 9.81169 8.41401 9.47592 7.99984 9.47587ZM8.62484 5.93095C8.62484 5.5858 8.34498 5.306 7.99984 5.30595C7.65485 5.30618 7.37484 5.58591 7.37484 5.93095V8.12919C7.37525 8.47388 7.6551 8.75397 7.99984 8.75419C8.34472 8.75415 8.62443 8.474 8.62484 8.12919V5.93095Z"),
            )
        }.build()
        return _ic_warning_16_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWarning16FilledPreview() {
    Icon(
        imageVector = Icons.ic_warning_16_filled,
        contentDescription = null,
    )
}