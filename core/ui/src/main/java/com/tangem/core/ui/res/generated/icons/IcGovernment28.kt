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

private var _ic_government_28: ImageVector? = null

val Icons.ic_government_28: ImageVector
    get() {
        if (_ic_government_28 != null) return _ic_government_28!!
        _ic_government_28 = ImageVector.Builder(
            name = "ic_government_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.6724 2.7901C13.9129 2.72765 14.168 2.73795 14.4038 2.82135L23.6733 6.10162C24.1723 6.27838 24.5063 6.74998 24.5063 7.27935L24.5195 9.80528C24.5195 10.4954 23.9596 11.055 23.2695 11.0553H23.0557V16.569C23.3321 16.6329 23.5817 16.79 23.7598 17.0182L24.7354 18.2682C24.9065 18.4877 24.9999 18.7584 25 19.0367V22.5042C25 23.1944 24.4401 23.7539 23.75 23.7542H4.25C3.55964 23.7542 3 23.1945 3 22.5042V19.0367C3.00009 18.7583 3.09341 18.4877 3.26465 18.2682L4.23926 17.0182L4.33398 16.9098C4.50109 16.74 4.71307 16.6232 4.94336 16.5699V11.0553H4.73047C4.04015 11.0553 3.48053 10.4956 3.48047 9.80528L3.46728 7.27935C3.46736 6.74988 3.80117 6.27831 4.30028 6.10162L13.5698 2.82135L13.6724 2.7901ZM5.5 19.4654V21.2542H22.5V19.4664L22.1641 19.0367H5.83496L5.5 19.4654ZM7.44336 16.5367H10.3457V11.0553H7.44336V16.5367ZM12.8457 16.5367H15.4658V11.0553H12.8457V16.5367ZM17.9658 16.5367H20.5557V11.0553H17.9658V16.5367ZM5.96728 8.16314L5.98047 8.55528H22.0195L22.0063 8.16314L13.9868 5.32623L5.96728 8.16314Z"),
            )
        }.build()
        return _ic_government_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGovernment28Preview() {
    Icon(
        imageVector = Icons.ic_government_28,
        contentDescription = null,
    )
}