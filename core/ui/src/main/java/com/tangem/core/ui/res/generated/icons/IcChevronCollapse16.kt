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

private var _ic_chevron_collapse_16: ImageVector? = null

val Icons.ic_chevron_collapse_16: ImageVector
    get() {
        if (_ic_chevron_collapse_16 != null) return _ic_chevron_collapse_16!!
        _ic_chevron_collapse_16 = ImageVector.Builder(
            name = "ic_chevron_collapse_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.94727 8.42773C7.29176 8.42849 7.57108 8.70826 7.57129 9.05273L7.57227 13.2656C7.57207 13.6106 7.29135 13.8906 6.94629 13.8906C6.60123 13.8906 6.32051 13.6106 6.32031 13.2656L6.32129 9.67773L2.73242 9.66992C2.38735 9.66918 2.10876 9.38905 2.10938 9.04395C2.11008 8.699 2.38946 8.41956 2.73438 8.41992L6.94727 8.42773ZM9.05274 2.10938C9.39777 2.10954 9.67774 2.3893 9.67774 2.73438V6.32227L13.2646 6.32129C13.6098 6.32129 13.8896 6.60117 13.8896 6.94629C13.8896 7.29142 13.6098 7.57129 13.2646 7.57129H9.05274C8.70766 7.5712 8.42779 7.29137 8.42774 6.94629V2.73438C8.42774 2.38925 8.70763 2.10946 9.05274 2.10938Z"),
            )
        }.build()
        return _ic_chevron_collapse_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronCollapse16Preview() {
    Icon(
        imageVector = Icons.ic_chevron_collapse_16,
        contentDescription = null,
    )
}