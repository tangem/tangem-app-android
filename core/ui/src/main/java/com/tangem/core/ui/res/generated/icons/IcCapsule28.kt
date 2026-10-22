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

private var _ic_capsule_28: ImageVector? = null

val Icons.ic_capsule_28: ImageVector
    get() {
        if (_ic_capsule_28 != null) return _ic_capsule_28!!
        _ic_capsule_28 = ImageVector.Builder(
            name = "ic_capsule_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.9571 5.13774C17.1401 2.95474 20.6793 2.95474 22.8623 5.13774C25.0453 7.32074 25.0453 10.86 22.8623 13.043L13.043 22.8623C10.86 25.0453 7.32074 25.0453 5.13774 22.8623C2.95474 20.6793 2.95474 17.1401 5.13774 14.9571L14.9571 5.13774ZM6.90532 16.7256C5.69872 17.9323 5.69866 19.8881 6.90532 21.0948C8.11198 22.3014 10.0678 22.3014 11.2745 21.0948L15.2998 17.0684L10.9307 12.6993L6.90532 16.7256ZM21.0948 6.90532C19.8881 5.69866 17.9323 5.69872 16.7256 6.90532L12.6993 10.9307L17.0684 15.2998L21.0948 11.2745C22.3014 10.0678 22.3014 8.11198 21.0948 6.90532Z"),
            )
        }.build()
        return _ic_capsule_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCapsule28Preview() {
    Icon(
        imageVector = Icons.ic_capsule_28,
        contentDescription = null,
    )
}