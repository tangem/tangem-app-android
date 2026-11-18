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

private var _ic_unlock_32_filled: ImageVector? = null

val Icons.ic_unlock_32_filled: ImageVector
    get() {
        if (_ic_unlock_32_filled != null) return _ic_unlock_32_filled!!
        _ic_unlock_32_filled = ImageVector.Builder(
            name = "ic_unlock_32_filled",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 2C19.7008 2.00019 22.6433 5.0609 22.6436 8.7627V11.2168C24.8216 11.3306 26.5 13.1644 26.5 15.3428V25.8691C26.4998 28.1184 24.7102 29.9999 22.4287 30H9.57129C7.28981 29.9999 5.50016 28.1184 5.5 25.8691V15.3428C5.5 13.0933 7.28971 11.211 9.57129 11.2109H19.6436V8.7627C19.6433 6.652 17.9789 5.00019 16 5C14.3294 5.00016 12.8873 6.17293 12.4746 7.81348C12.2725 8.61671 11.4566 9.10421 10.6533 8.90234C9.85005 8.70017 9.36239 7.88438 9.56445 7.08105C10.2938 4.18217 12.8787 2.00016 16 2Z"),
            )
        }.build()
        return _ic_unlock_32_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUnlock32FilledPreview() {
    Icon(
        imageVector = Icons.ic_unlock_32_filled,
        contentDescription = null,
    )
}