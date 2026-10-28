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

private var _ic_hockey_16: ImageVector? = null

val Icons.ic_hockey_16: ImageVector
    get() {
        if (_ic_hockey_16 != null) return _ic_hockey_16!!
        _ic_hockey_16 = ImageVector.Builder(
            name = "ic_hockey_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.87724 2.24409C8.94447 1.88845 10.1228 1.85331 10.8919 2.60346C10.8983 2.60933 10.9062 2.61388 10.9124 2.62006C10.9189 2.62659 10.9245 2.63398 10.931 2.64057L13.3792 5.08881C14.1478 5.85739 14.1135 7.04642 13.7552 8.12202C13.3835 9.23741 12.6041 10.4508 11.5276 11.5273C10.4511 12.6038 9.23775 13.3831 8.12236 13.7548C7.04676 14.1132 5.85772 14.1474 5.08915 13.3789L2.6204 10.9101C1.85224 10.1414 1.88605 8.95235 2.24443 7.8769C2.61616 6.76159 3.39558 5.54801 4.47197 4.47163C5.54835 3.39524 6.76193 2.61582 7.87724 2.24409ZM11.4671 4.94428C11.4247 5.18207 11.3649 5.42069 11.2874 5.65327C10.9157 6.7686 10.1372 7.9831 9.06083 9.05952C7.98437 10.1359 6.76994 10.9144 5.65458 11.2861C5.42157 11.3637 5.18286 11.4243 4.94462 11.4667L5.97294 12.4951C6.22636 12.7485 6.79217 12.8797 7.72685 12.5683C8.62202 12.27 9.67489 11.6124 10.6438 10.6435C11.6128 9.67455 12.2703 8.62167 12.5686 7.72651C12.88 6.79184 12.7488 6.22603 12.4954 5.9726L11.4671 4.94428ZM10.0267 3.50385C9.77297 3.2507 9.20705 3.11928 8.27275 3.43061C7.37764 3.72895 6.3246 4.38657 5.35575 5.35541C4.38691 6.32426 3.72929 7.3773 3.43095 8.27241C3.12042 9.20432 3.25036 9.76973 3.50224 10.0244L3.50615 10.0273C3.75985 10.2804 4.32579 10.4118 5.26005 10.1005C6.15515 9.80222 7.20819 9.14455 8.17704 8.17573C9.14584 7.20693 9.80348 6.15382 10.1018 5.25873C10.4114 4.32984 10.2828 3.76554 10.0325 3.50971L10.0267 3.50385Z"),
            )
        }.build()
        return _ic_hockey_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHockey16Preview() {
    Icon(
        imageVector = Icons.ic_hockey_16,
        contentDescription = null,
    )
}