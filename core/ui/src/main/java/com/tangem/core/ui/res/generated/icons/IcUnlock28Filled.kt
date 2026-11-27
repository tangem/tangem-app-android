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

private var _ic_unlock_28_filled: ImageVector? = null

val Icons.ic_unlock_28_filled: ImageVector
    get() {
        if (_ic_unlock_28_filled != null) return _ic_unlock_28_filled!!
        _ic_unlock_28_filled = ImageVector.Builder(
            name = "ic_unlock_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2C17.1615 2.00013 19.6787 4.6123 19.6787 7.77637V9.92383C21.5493 10.0001 22.9998 11.5662 23 13.4336V22.4863C23 24.4011 21.4753 25.9998 19.5361 26H8.46387C6.52473 25.9998 5 24.4011 5 22.4863V13.4336C5.00025 11.5191 6.52489 9.92113 8.46387 9.9209H17.1787V7.77637C17.1787 5.94128 15.7296 4.50013 14 4.5C12.54 4.5 11.2823 5.52326 10.9229 6.9502C10.7542 7.61946 10.0746 8.02491 9.40527 7.85645C8.7362 7.68767 8.33066 7.00904 8.49902 6.33984C9.12298 3.86254 11.3333 2 14 2Z"),
            )
        }.build()
        return _ic_unlock_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUnlock28FilledPreview() {
    Icon(
        imageVector = Icons.ic_unlock_28_filled,
        contentDescription = null,
    )
}