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

private var _ic_percent_12: ImageVector? = null

val Icons.ic_percent_12: ImageVector
    get() {
        if (_ic_percent_12 != null) return _ic_percent_12!!
        _ic_percent_12 = ImageVector.Builder(
            name = "ic_percent_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.37007 7.36992C8.08592 6.65418 9.24694 6.65428 9.96285 7.36992L10.0878 7.50859C10.6755 8.22859 10.6341 9.29141 9.96285 9.96269C9.24704 10.6784 8.086 10.6782 7.37007 9.96269C6.65438 9.24679 6.65424 8.08574 7.37007 7.36992ZM9.19234 8.01933C8.8651 7.75264 8.38209 7.77204 8.07711 8.07695C7.75185 8.40219 7.7521 8.92929 8.07711 9.25469C8.4025 9.58006 8.93043 9.58006 9.25582 9.25469C9.56033 8.94967 9.5791 8.46748 9.31246 8.14043L9.25582 8.07695L9.19234 8.01933Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.97945 2.31328C9.17472 2.1182 9.49128 2.11809 9.68648 2.31328C9.88146 2.50851 9.8815 2.82511 9.68648 3.02031L3.02046 9.68633C2.8253 9.88136 2.50868 9.88121 2.31343 9.68633C2.11822 9.4911 2.11829 9.17456 2.31343 8.97929L8.97945 2.31328Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M2.03707 2.03691C2.75298 1.32103 3.91393 1.32103 4.62984 2.03691L4.75484 2.17558C5.34231 2.89561 5.30109 3.95846 4.62984 4.62969C3.91409 5.34535 2.753 5.34504 2.03707 4.62969C1.32115 3.9138 1.32117 2.75281 2.03707 2.03691ZM3.85933 2.68633C3.53207 2.41941 3.04915 2.4389 2.7441 2.74394C2.41878 3.06926 2.41887 3.5963 2.7441 3.92168C3.0695 4.24686 3.59748 4.24698 3.92281 3.92168C4.22733 3.61673 4.24594 3.1345 3.97945 2.80742L3.92281 2.74394L3.85933 2.68633Z"),
            )
        }.build()
        return _ic_percent_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPercent12Preview() {
    Icon(
        imageVector = Icons.ic_percent_12,
        contentDescription = null,
    )
}