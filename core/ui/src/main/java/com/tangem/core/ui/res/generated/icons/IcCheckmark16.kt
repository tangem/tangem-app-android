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

private var _ic_checkmark_16: ImageVector? = null

val Icons.ic_checkmark_16: ImageVector
    get() {
        if (_ic_checkmark_16 != null) return _ic_checkmark_16!!
        _ic_checkmark_16 = ImageVector.Builder(
            name = "ic_checkmark_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.5537 4.06253C12.7953 3.8161 13.191 3.81222 13.4375 4.05374C13.6839 4.29533 13.6878 4.69104 13.4463 4.93753L6.58792 11.9375C6.47042 12.0573 6.30946 12.125 6.14164 12.125C5.97381 12.125 5.81284 12.0574 5.69535 11.9375L2.55375 8.73148C2.31219 8.48498 2.31612 8.08929 2.56253 7.84769C2.80906 7.60611 3.20473 7.60998 3.44632 7.85648L6.14066 10.6065L12.5537 4.06253Z"),
            )
        }.build()
        return _ic_checkmark_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCheckmark16Preview() {
    Icon(
        imageVector = Icons.ic_checkmark_16,
        contentDescription = null,
    )
}