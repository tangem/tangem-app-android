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

private var _ic_bag_16: ImageVector? = null

val Icons.ic_bag_16: ImageVector
    get() {
        if (_ic_bag_16 != null) return _ic_bag_16!!
        _ic_bag_16 = ImageVector.Builder(
            name = "ic_bag_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.79102 6.18066C10.1362 6.18066 10.416 6.46049 10.416 6.80566V7.40332C10.4158 8.65428 9.46538 9.68273 8.24707 9.80664L8 9.81934L7.75293 9.80664C6.5343 9.68304 5.58322 8.65451 5.58301 7.40332V6.80566C5.58301 6.46049 5.86283 6.18066 6.20801 6.18066C6.55319 6.18066 6.83301 6.46049 6.83301 6.80566V7.40332C6.83324 8.04748 7.35583 8.56934 8 8.56934C8.64401 8.56915 9.16579 8.04737 9.16602 7.40332V6.80566C9.16602 6.46059 9.44597 6.18083 9.79102 6.18066Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.583 2C12.5877 2.00015 13.4023 2.81464 13.4023 3.81934V4.26562C13.7687 4.59844 14 5.07744 14 5.61133V12.1807C13.9999 13.1853 13.1854 13.9999 12.1807 14H3.81934C2.8146 13.9999 2.00013 13.1854 2 12.1807V5.61133C2.00004 5.07769 2.23068 4.59842 2.59668 4.26562V3.81934C2.59675 2.81458 3.41124 2.00005 4.41602 2H11.583ZM3.81934 5.04199C3.50495 5.04205 3.25007 5.2969 3.25 5.61133V12.1807C3.25013 12.495 3.50499 12.7499 3.81934 12.75H12.1807C12.495 12.7499 12.7499 12.495 12.75 12.1807V5.61133C12.7499 5.29693 12.495 5.04209 12.1807 5.04199H3.81934ZM4.41602 3.25C4.11083 3.25005 3.86399 3.49032 3.84961 3.79199H12.1494C12.135 3.49038 11.8881 3.25015 11.583 3.25H4.41602Z"),
            )
        }.build()
        return _ic_bag_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBag16Preview() {
    Icon(
        imageVector = Icons.ic_bag_16,
        contentDescription = null,
    )
}