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

private var _ic_capsule_24: ImageVector? = null

val Icons.ic_capsule_24: ImageVector
    get() {
        if (_ic_capsule_24 != null) return _ic_capsule_24!!
        _ic_capsule_24 = ImageVector.Builder(
            name = "ic_capsule_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.8605 4.35369C14.7191 2.49549 17.7325 2.49528 19.591 4.35369C21.4495 6.21216 21.4492 9.22554 19.591 11.0842L11.0842 19.591C9.22554 21.4492 6.21216 21.4495 4.35369 19.591C2.49528 17.7325 2.49549 14.7191 4.35369 12.8605L12.8605 4.35369ZM5.76873 14.2746C4.69119 15.3521 4.69119 17.0994 5.76873 18.1769C6.84618 19.2539 8.59267 19.254 9.6701 18.1769L13.215 14.6301L9.31365 10.7287L5.76873 14.2746ZM18.1769 5.76873C17.0994 4.69119 15.3521 4.69119 14.2746 5.76873L10.7287 9.31365L14.6301 13.215L18.1769 9.6701C19.254 8.59267 19.2539 6.84618 18.1769 5.76873Z"),
            )
        }.build()
        return _ic_capsule_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCapsule24Preview() {
    Icon(
        imageVector = Icons.ic_capsule_24,
        contentDescription = null,
    )
}