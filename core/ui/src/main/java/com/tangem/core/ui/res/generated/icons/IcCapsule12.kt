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

private var _ic_capsule_12: ImageVector? = null

val Icons.ic_capsule_12: ImageVector
    get() {
        if (_ic_capsule_12 != null) return _ic_capsule_12!!
        _ic_capsule_12 = ImageVector.Builder(
            name = "ic_capsule_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.44221 2.19636C7.37038 1.2682 8.87536 1.26821 9.80354 2.19636C10.7317 3.12454 10.7317 4.62951 9.80354 5.55769L5.55745 9.80378C4.62927 10.732 3.12429 10.732 2.19612 9.80378C1.26797 8.87561 1.26795 7.37062 2.19612 6.44245L6.44221 2.19636ZM2.90315 7.14949C2.36551 7.68713 2.36553 8.5591 2.90315 9.09675C3.4408 9.6344 4.31276 9.6344 4.85041 9.09675L6.61995 7.32722L4.67268 5.37995L2.90315 7.14949ZM9.09651 2.90339C8.55885 2.36577 7.68688 2.36575 7.14924 2.90339L5.37971 4.67292L7.32698 6.62019L9.09651 4.85066C9.63416 4.31301 9.63416 3.44104 9.09651 2.90339Z"),
            )
        }.build()
        return _ic_capsule_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCapsule12Preview() {
    Icon(
        imageVector = Icons.ic_capsule_12,
        contentDescription = null,
    )
}