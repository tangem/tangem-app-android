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

private var _ic_capsule_20: ImageVector? = null

val Icons.ic_capsule_20: ImageVector
    get() {
        if (_ic_capsule_20 != null) return _ic_capsule_20!!
        _ic_capsule_20 = ImageVector.Builder(
            name = "ic_capsule_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.7139 4.07817C12.152 2.64037 14.4839 2.6402 15.9219 4.07817C17.3599 5.51615 17.3597 7.8481 15.9219 9.28618L9.28618 15.9219C7.8481 17.3597 5.51615 17.3599 4.07817 15.9219C2.6402 14.4839 2.64037 12.152 4.07817 10.7139L10.7139 4.07817ZM5.1397 11.7745C4.28742 12.6267 4.28743 14.0081 5.1397 14.8604C5.99197 15.7127 7.37336 15.7127 8.22563 14.8604L11.0127 12.0733L7.92681 8.98735L5.1397 11.7745ZM14.8604 5.1397C14.0081 4.28743 12.6267 4.28742 11.7745 5.1397L8.98735 7.92681L12.0733 11.0127L14.8604 8.22563C15.7127 7.37336 15.7127 5.99197 14.8604 5.1397Z"),
            )
        }.build()
        return _ic_capsule_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCapsule20Preview() {
    Icon(
        imageVector = Icons.ic_capsule_20,
        contentDescription = null,
    )
}