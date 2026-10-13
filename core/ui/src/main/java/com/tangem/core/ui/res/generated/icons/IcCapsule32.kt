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

private var _ic_capsule_32: ImageVector? = null

val Icons.ic_capsule_32: ImageVector
    get() {
        if (_ic_capsule_32 != null) return _ic_capsule_32!!
        _ic_capsule_32 = ImageVector.Builder(
            name = "ic_capsule_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.0291 5.88194C19.5388 3.37242 23.6084 3.37231 26.118 5.88194C28.6276 8.39155 28.6275 12.4611 26.118 14.9708L14.9706 26.1183C12.4609 28.6278 8.39132 28.6279 5.88169 26.1183C3.37209 23.6086 3.37215 19.5391 5.88169 17.0294L17.0291 5.88194ZM8.00376 19.1505C6.66567 20.4886 6.6657 22.6581 8.00376 23.9962C9.34185 25.3343 11.5114 25.3343 12.8495 23.9962L17.3622 19.4835L12.5165 14.6378L8.00376 19.1505ZM23.9959 8.00401C22.6579 6.66591 20.4883 6.66591 19.1502 8.00401L14.6375 12.5167L19.4832 17.3624L23.9959 12.8497C25.334 11.5116 25.334 9.34211 23.9959 8.00401Z"),
            )
        }.build()
        return _ic_capsule_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCapsule32Preview() {
    Icon(
        imageVector = Icons.ic_capsule_32,
        contentDescription = null,
    )
}