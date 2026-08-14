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

private var _ic_filter_20: ImageVector? = null

val Icons.ic_filter_20: ImageVector
    get() {
        if (_ic_filter_20 != null) return _ic_filter_20!!
        _ic_filter_20 = ImageVector.Builder(
            name = "ic_filter_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.82454 10.7024C9.41669 10.7026 10.7435 11.8192 11.0824 13.3098H16.6663C17.0806 13.3098 17.4163 13.6456 17.4163 14.0598C17.4162 14.4739 17.0805 14.8098 16.6663 14.8098H11.0814C10.7423 16.3001 9.41644 17.416 7.82454 17.4163C6.23244 17.4163 4.90588 16.3002 4.56673 14.8098H3.33333C2.91921 14.8098 2.58348 14.4739 2.58333 14.0598C2.58333 13.6456 2.91912 13.3098 3.33333 13.3098H4.56576C4.90464 11.819 6.23218 10.7024 7.82454 10.7024ZM7.82454 12.2024C6.81084 12.2024 5.98177 13.0302 5.98177 14.0598C5.98192 15.0893 6.81093 15.9163 7.82454 15.9163C8.83794 15.916 9.6662 15.0891 9.66634 14.0598C9.66634 13.0304 8.83803 12.2026 7.82454 12.2024ZM11.9906 2.58325C13.5829 2.58325 14.9095 3.69994 15.2484 5.19067H16.6663C17.0806 5.19068 17.4163 5.52646 17.4163 5.94067C17.4162 6.35475 17.0805 6.69068 16.6663 6.69067H15.2484C14.9092 8.18107 13.5827 9.29712 11.9906 9.29712C10.3986 9.29703 9.07291 8.18095 8.73372 6.69067H3.33333C2.91923 6.69067 2.5835 6.35474 2.58333 5.94067C2.58333 5.52646 2.91912 5.19068 3.33333 5.19067H8.73275C9.07161 3.69996 10.3983 2.58334 11.9906 2.58325ZM11.9906 4.08325C10.9769 4.08335 10.1478 4.91116 10.1478 5.94067C10.148 6.97003 10.9771 7.79702 11.9906 7.79712C13.0041 7.79712 13.8331 6.97009 13.8333 5.94067C13.8333 4.9111 13.0043 4.08325 11.9906 4.08325Z"),
            )
        }.build()
        return _ic_filter_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcFilter20Preview() {
    Icon(
        imageVector = Icons.ic_filter_20,
        contentDescription = null,
    )
}