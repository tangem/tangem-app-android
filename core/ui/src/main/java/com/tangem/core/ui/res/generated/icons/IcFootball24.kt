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

private var _ic_football_24: ImageVector? = null

val Icons.ic_football_24: ImageVector
    get() {
        if (_ic_football_24 != null) return _ic_football_24!!
        _ic_football_24 = ImageVector.Builder(
            name = "ic_football_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.9278 4.9278C8.83381 1.02179 15.1663 1.02179 19.0723 4.9278L19.4268 5.30085C22.9743 9.22714 22.8563 15.2884 19.0723 19.0723C15.1664 22.9783 8.8338 22.9782 4.9278 19.0723C1.02179 15.1663 1.02179 8.83378 4.9278 4.9278ZM10.2335 16.1407L9.66803 16.919L10.7383 19.8995C11.5738 20.0322 12.4253 20.0321 13.2608 19.8995L14.3311 16.919L13.7667 16.1407H10.2335ZM6.38092 17.6954C6.97094 18.2776 7.62897 18.7487 8.32917 19.1104L7.83698 17.7393L6.38092 17.6954ZM16.1622 17.7393L15.669 19.1104C16.3696 18.7487 17.0279 18.2778 17.6182 17.6954L16.1622 17.7393ZM4.09772 13.254C4.22807 14.0797 4.48754 14.8886 4.87702 15.6485L8.04987 15.7442L8.6153 14.9639L7.52545 11.6104L6.60651 11.3116L4.09772 13.254ZM16.4727 11.6104L15.3829 14.9639L15.9493 15.7442L19.1212 15.6485C19.5107 14.8887 19.771 14.0805 19.9014 13.2549L17.3907 11.3116L16.4727 11.6104ZM9.49323 11.1954L10.4512 14.1407H13.5489L14.5049 11.1954L11.9991 9.37409L9.49323 11.1954ZM18.7325 9.82135L19.8995 10.7247C19.7709 9.92415 19.5188 9.1405 19.1465 8.40143L18.7325 9.82135ZM4.85163 8.40241C4.48036 9.13982 4.23028 9.92216 4.10163 10.7208L5.26569 9.82038L4.85163 8.40241ZM8.36237 4.87213C7.63448 5.2438 6.95097 5.73275 6.34186 6.34186C6.33975 6.34398 6.33714 6.3456 6.33503 6.34772L7.22956 9.41022L8.14069 9.70612L10.9991 7.62995V6.66706L8.36237 4.87213ZM12.9991 6.66803V7.62799L15.8585 9.70612L16.7686 9.4112L17.6632 6.34674L17.6583 6.34186L17.3594 6.05768C16.8287 5.5782 16.248 5.18418 15.6368 4.87213L12.9991 6.66803ZM13.2266 4.09284C12.4133 3.96734 11.5848 3.96813 10.7715 4.09381L11.9991 4.92975L13.2266 4.09284Z"),
            )
        }.build()
        return _ic_football_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcFootball24Preview() {
    Icon(
        imageVector = Icons.ic_football_24,
        contentDescription = null,
    )
}