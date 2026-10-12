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

private var _ic_rocket_16: ImageVector? = null

val Icons.ic_rocket_16: ImageVector
    get() {
        if (_ic_rocket_16 != null) return _ic_rocket_16!!
        _ic_rocket_16 = ImageVector.Builder(
            name = "ic_rocket_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.57011 9.58105C5.07968 9.50449 5.59633 9.67475 5.96073 10.0391C6.32493 10.4034 6.49416 10.9193 6.41777 11.4287C6.31181 12.1338 5.75859 12.6879 5.05351 12.7939L3.71562 12.9951C3.51955 13.0245 3.32065 12.9586 3.18046 12.8184C3.04065 12.6783 2.9754 12.4799 3.00468 12.2842L3.20585 10.9463C3.3119 10.2413 3.86513 9.68712 4.57011 9.58105ZM4.75663 10.8174C4.59433 10.8418 4.46677 10.9696 4.44218 11.1318L4.36601 11.6328L4.86698 11.5576C5.02945 11.5332 5.15703 11.4056 5.18144 11.2432C5.19899 11.1258 5.15986 11.0067 5.07597 10.9229C4.99214 10.8393 4.87368 10.7999 4.75663 10.8174Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.8904 2.99805C12.5042 2.99805 13.0017 3.49561 13.0017 4.10938C13.0017 5.836 12.2171 7.46914 10.8689 8.54785L10.5701 8.78613V10.6172C10.5701 11.0381 10.3323 11.4231 9.95585 11.6113L8.78105 12.1982C8.4929 12.3423 8.15582 12.3547 7.85819 12.2314C7.5607 12.1081 7.33113 11.8612 7.22929 11.5557L6.94804 10.7109C6.88317 10.669 6.8199 10.6219 6.76347 10.5654L5.43437 9.23633C5.37791 9.17986 5.3308 9.11664 5.28886 9.05176L4.44316 8.76953C4.13766 8.66761 3.8916 8.43817 3.76835 8.14062C3.64516 7.84307 3.65659 7.50682 3.80058 7.21875L4.38847 6.04395L4.46757 5.90918C4.67306 5.61149 5.01428 5.4297 5.38261 5.42969H7.21269L7.45194 5.13086C8.53064 3.78272 10.1638 2.99808 11.8904 2.99805ZM11.7478 4.25098C10.4539 4.29257 9.23925 4.89746 8.42753 5.91211L8.00077 6.44531C7.88221 6.59319 7.70301 6.6796 7.51347 6.67969H5.46757L4.99003 7.63379L5.69706 7.87012C5.73195 7.876 5.76704 7.88434 5.80155 7.89648L5.94902 7.94824C6.1449 8.01718 6.29072 8.17927 6.34355 8.37793L7.62382 9.65723C7.80947 9.71234 7.96038 9.84967 8.03007 10.0322L8.09745 10.209L8.10624 10.2334L8.36405 11.0078L9.32011 10.5303V8.48633C9.32011 8.29651 9.4063 8.11665 9.55448 7.99805L10.0877 7.57227L10.2801 7.40918C11.1789 6.60233 11.7092 5.46138 11.7478 4.25098Z"),
            )
        }.build()
        return _ic_rocket_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcRocket16Preview() {
    Icon(
        imageVector = Icons.ic_rocket_16,
        contentDescription = null,
    )
}