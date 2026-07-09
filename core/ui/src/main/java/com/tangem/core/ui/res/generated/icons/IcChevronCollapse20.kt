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

private var _ic_chevron_collapse_20: ImageVector? = null

val Icons.ic_chevron_collapse_20: ImageVector
    get() {
        if (_ic_chevron_collapse_20 != null) return _ic_chevron_collapse_20!!
        _ic_chevron_collapse_20 = ImageVector.Builder(
            name = "ic_chevron_collapse_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.55762 10.6816C8.97099 10.6824 9.30616 11.0184 9.30664 11.4316V17.1846C9.30638 17.5986 8.97069 17.9336 8.55664 17.9336C8.14259 17.9336 7.80691 17.5986 7.80664 17.1846V12.1816L2.80371 12.1729C2.38955 12.1721 2.05396 11.8351 2.05469 11.4209C2.05548 11.007 2.39183 10.6715 2.80567 10.6719L8.55762 10.6816ZM11.4326 2.05469C11.8466 2.05485 12.1824 2.39076 12.1826 2.80469V7.80762L17.1846 7.80664C17.5985 7.80696 17.9345 8.14265 17.9346 8.55664C17.9345 8.97062 17.5985 9.30632 17.1846 9.30664H11.4316C11.0178 9.30622 10.6817 8.97055 10.6816 8.55664L10.6826 2.80469C10.6828 2.39069 11.0186 2.05474 11.4326 2.05469Z"),
            )
        }.build()
        return _ic_chevron_collapse_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronCollapse20Preview() {
    Icon(
        imageVector = Icons.ic_chevron_collapse_20,
        contentDescription = null,
    )
}