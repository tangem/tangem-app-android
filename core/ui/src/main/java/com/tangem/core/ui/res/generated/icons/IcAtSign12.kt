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

private var _ic_at_sign_12: ImageVector? = null

val Icons.ic_at_sign_12: ImageVector
    get() {
        if (_ic_at_sign_12 != null) return _ic_at_sign_12!!
        _ic_at_sign_12 = ImageVector.Builder(
            name = "ic_at_sign_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.5 6C7.5 5.17164 6.82836 4.5 6 4.5C5.17164 4.5 4.5 5.17164 4.5 6C4.5 6.82836 5.17164 7.5 6 7.5C6.82836 7.5 7.5 6.82836 7.5 6ZM8.5 6.75C8.5 7.16436 8.83564 7.5 9.25 7.5C9.66436 7.5 10 7.16436 10 6.75V6C10 3.79064 8.20936 2 6 2C3.79064 2 2 3.79064 2 6C2 8.20936 3.79064 10 6 10C6.51086 10 7.02942 9.90282 7.53027 9.69531C7.79821 9.58426 8.04843 9.44417 8.28125 9.28223C8.5078 9.1249 8.81887 9.18094 8.97656 9.40723C9.13424 9.63392 9.07826 9.94583 8.85156 10.1035C8.56548 10.3025 8.25302 10.4782 7.91309 10.6191C7.28705 10.8785 6.63802 11 6 11C3.23836 11 1 8.76164 1 6C1 3.23836 3.23836 1 6 1C8.76164 1 11 3.23836 11 6V6.75C11 7.71664 10.2166 8.5 9.25 8.5C8.64663 8.5 8.11442 8.19495 7.7998 7.73047C7.34494 8.20344 6.70798 8.5 6 8.5C4.61936 8.5 3.5 7.38064 3.5 6C3.5 4.61936 4.61936 3.5 6 3.5C7.38064 3.5 8.5 4.61936 8.5 6V6.75Z"),
            )
        }.build()
        return _ic_at_sign_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtSign12Preview() {
    Icon(
        imageVector = Icons.ic_at_sign_12,
        contentDescription = null,
    )
}