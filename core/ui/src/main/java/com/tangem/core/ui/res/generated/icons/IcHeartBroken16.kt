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

private var _ic_heart_broken_16: ImageVector? = null

val Icons.ic_heart_broken_16: ImageVector
    get() {
        if (_ic_heart_broken_16 != null) return _ic_heart_broken_16!!
        _ic_heart_broken_16 = ImageVector.Builder(
            name = "ic_heart_broken_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.2119 2.5C12.5521 2.50031 14.0029 4.70655 14.0029 6.63379C14.0028 7.65361 13.6105 8.59271 13.0713 9.40234C12.5316 10.2126 11.8223 10.9278 11.1299 11.5156C10.435 12.1054 9.73935 12.5815 9.21191 12.9102C8.94794 13.0746 8.72349 13.2043 8.55859 13.2939C8.47673 13.3385 8.40636 13.3747 8.35156 13.4014C8.32501 13.4143 8.29574 13.4279 8.26855 13.4395C8.25576 13.4449 8.23542 13.4529 8.21191 13.4609C8.20024 13.4649 8.18004 13.4714 8.15527 13.4775C8.14029 13.4813 8.07935 13.497 8.00098 13.4971C7.92349 13.497 7.8634 13.4815 7.84766 13.4775C7.82308 13.4714 7.80276 13.4649 7.79102 13.4609C7.76739 13.4529 7.74624 13.4449 7.7334 13.4395C7.70641 13.428 7.67774 13.4142 7.65137 13.4014C7.59665 13.3747 7.52608 13.3384 7.44434 13.2939C7.27944 13.2043 7.05403 13.0746 6.79004 12.9102C6.26273 12.5816 5.56768 12.1052 4.87305 11.5156C4.18068 10.9279 3.47133 10.2124 2.93164 9.40234C2.39241 8.59271 2.0001 7.65361 2 6.63379C2.00005 4.70644 3.45057 2.50005 5.79102 2.5C6.76206 2.50006 7.48919 2.86673 8.00098 3.29004C8.51283 2.86653 9.24045 2.5 10.2119 2.5ZM5.79102 3.75C4.34424 3.75005 3.25005 5.1742 3.25 6.63379C3.2501 7.32849 3.51797 8.02773 3.97168 8.70898C4.42501 9.38953 5.04224 10.0197 5.68164 10.5625C6.31859 11.1031 6.96175 11.5436 7.45117 11.8486C7.48253 11.8682 7.51395 11.886 7.54395 11.9043L8.2041 9.66016L6.91992 8.08887C6.73201 7.85885 6.73214 7.52794 6.91992 7.29785L8.16016 5.78027L7.60254 4.64453C7.57508 4.6216 7.54789 4.59802 7.52441 4.57031C7.18932 4.17412 6.63819 3.75008 5.79102 3.75ZM10.2119 3.75C9.5951 3.75 9.13554 3.97487 8.80176 4.25L9.45898 5.59082C9.56625 5.80992 9.53593 6.07166 9.38184 6.26074L8.21094 7.69238L9.38184 9.125C9.5123 9.28474 9.55517 9.49938 9.49707 9.69727L8.9375 11.5986C9.3564 11.3201 9.84091 10.9703 10.3213 10.5625C10.9607 10.0197 11.5779 9.38958 12.0312 8.70898C12.4849 8.02775 12.7528 7.32846 12.7529 6.63379C12.7529 5.17434 11.6585 3.75032 10.2119 3.75Z"),
            )
        }.build()
        return _ic_heart_broken_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeartBroken16Preview() {
    Icon(
        imageVector = Icons.ic_heart_broken_16,
        contentDescription = null,
    )
}