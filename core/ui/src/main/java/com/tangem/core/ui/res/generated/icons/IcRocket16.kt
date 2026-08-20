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
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M4.57011 9.5809C5.07968 9.50433 5.59633 9.6746 5.96073 10.0389C6.32493 10.4032 6.49416 10.9191 6.41777 11.4286C6.31181 12.1337 5.75859 12.6877 5.05351 12.7938L3.71562 12.995C3.51955 13.0244 3.32065 12.9584 3.18046 12.8182C3.04065 12.6781 2.9754 12.4798 3.00468 12.284L3.20585 10.9461C3.3119 10.2411 3.86513 9.68697 4.57011 9.5809ZM4.75663 10.8172C4.59433 10.8416 4.46677 10.9695 4.44218 11.1317L4.36601 11.6327L4.86698 11.5575C5.02945 11.5331 5.15703 11.4055 5.18144 11.243C5.19899 11.1257 5.15986 11.0066 5.07597 10.9227C4.99214 10.8392 4.87368 10.7998 4.75663 10.8172Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M11.8904 2.99789C12.5042 2.99789 13.0018 3.49546 13.0018 4.10922C13.0017 5.83584 12.2171 7.46899 10.8689 8.5477L10.5701 8.78598V10.617C10.5701 11.038 10.3323 11.4229 9.95585 11.6112L8.78105 12.1981C8.4929 12.3421 8.15582 12.3546 7.8582 12.2313C7.5607 12.108 7.33113 11.861 7.22929 11.5555L6.94804 10.7108C6.88317 10.6688 6.8199 10.6217 6.76347 10.5653L5.43437 9.23618C5.37791 9.17971 5.3308 9.11648 5.28886 9.05161L4.44316 8.76938C4.13766 8.66746 3.8916 8.43802 3.76835 8.14047C3.64516 7.84292 3.65659 7.50667 3.80058 7.2186L4.38847 6.04379L4.46757 5.90903C4.67306 5.61134 5.01429 5.42955 5.38261 5.42954H7.21269L7.45195 5.13071C8.53064 3.78257 10.1638 2.99792 11.8904 2.99789ZM11.7478 4.25082C10.4539 4.29241 9.23925 4.89731 8.42753 5.91196L8.00077 6.44516C7.88221 6.59304 7.70301 6.67945 7.51347 6.67954H5.46757L4.99003 7.63364L5.69706 7.86997C5.73195 7.87585 5.76704 7.88419 5.80155 7.89633L5.94902 7.94809C6.1449 8.01703 6.29072 8.17912 6.34355 8.37778L7.62382 9.65707C7.80947 9.71218 7.96038 9.84951 8.03007 10.0321L8.09745 10.2088L8.10624 10.2332L8.36405 11.0077L9.32011 10.5301V8.48618C9.32011 8.29636 9.4063 8.1165 9.55448 7.99789L10.0877 7.57211L10.2801 7.40903C11.1789 6.60218 11.7092 5.46123 11.7478 4.25082Z"),
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