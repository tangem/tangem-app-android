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

private var _ic_unlock_32: ImageVector? = null

val Icons.ic_unlock_32: ImageVector
    get() {
        if (_ic_unlock_32 != null) return _ic_unlock_32!!
        _ic_unlock_32 = ImageVector.Builder(
            name = "ic_unlock_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 2C19.7008 2.00019 22.6433 5.0609 22.6436 8.7627V11.2168C24.8216 11.3306 26.5 13.1644 26.5 15.3428V25.8691C26.4998 28.1184 24.7102 29.9999 22.4287 30H9.57129C7.28981 29.9999 5.50016 28.1184 5.5 25.8691V15.3428C5.5 13.0933 7.28971 11.211 9.57129 11.2109H19.6436V8.7627C19.6433 6.652 17.9789 5.00019 16 5C14.3294 5.00016 12.8873 6.17293 12.4746 7.81348C12.2725 8.61671 11.4566 9.10421 10.6533 8.90234C9.85005 8.70017 9.36239 7.88438 9.56445 7.08105C10.2938 4.18217 12.8787 2.00016 16 2ZM9.57129 14.2109C9.01156 14.211 8.5 14.6844 8.5 15.3428V25.8691C8.50016 26.5273 9.01165 26.9999 9.57129 27H22.4287C22.9883 26.9999 23.4998 26.5273 23.5 25.8691V15.3428C23.5 14.6844 22.9884 14.211 22.4287 14.2109H9.57129Z"),
            )
        }.build()
        return _ic_unlock_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUnlock32Preview() {
    Icon(
        imageVector = Icons.ic_unlock_32,
        contentDescription = null,
    )
}