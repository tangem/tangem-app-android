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

private var _ic_warning_16: ImageVector? = null

val Icons.ic_warning_16: ImageVector
    get() {
        if (_ic_warning_16 != null) return _ic_warning_16!!
        _ic_warning_16 = ImageVector.Builder(
            name = "ic_warning_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99984 9.47587C8.41401 9.47592 8.74984 9.81169 8.74984 10.2259C8.74951 10.6398 8.41381 10.9758 7.99984 10.9759C7.58599 10.9757 7.25017 10.6397 7.24984 10.2259C7.24984 9.81178 7.58579 9.47607 7.99984 9.47587Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99984 5.30595C8.34498 5.306 8.62484 5.5858 8.62484 5.93095V8.12919C8.62443 8.474 8.34472 8.75415 7.99984 8.75419C7.6551 8.75397 7.37525 8.47388 7.37484 8.12919V5.93095C7.37484 5.58591 7.65485 5.30618 7.99984 5.30595Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.36702 3.20146C7.08883 1.93351 8.91183 1.93349 9.63363 3.20146L13.7498 10.427C14.4628 11.6803 13.5637 13.246 12.117 13.2464H3.88363C2.4365 13.2462 1.53656 11.6807 2.25081 10.427L6.36702 3.20146ZM8.54769 3.81962C8.30528 3.39381 7.69538 3.39382 7.45296 3.81962L3.33675 11.0462C3.09451 11.472 3.40199 11.9962 3.88363 11.9964H12.117C12.5976 11.996 12.9057 11.4722 12.6639 11.0462L8.54769 3.81962Z"),
            )
        }.build()
        return _ic_warning_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWarning16Preview() {
    Icon(
        imageVector = Icons.ic_warning_16,
        contentDescription = null,
    )
}