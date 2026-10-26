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

private var _ic_bone_12: ImageVector? = null

val Icons.ic_bone_12: ImageVector
    get() {
        if (_ic_bone_12 != null) return _ic_bone_12!!
        _ic_bone_12 = ImageVector.Builder(
            name = "ic_bone_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M1.5 7.21875C1.50019 6.2852 2.25672 5.52838 3.19043 5.52832H4.11719C4.12199 5.5283 4.12739 5.52592 4.13086 5.52246L5.52441 4.12598C5.52763 4.12276 5.53026 4.11793 5.53027 4.11328V3.19141C5.53027 2.25766 6.28683 1.5 7.2207 1.5C8.12054 1.50018 8.85348 2.20332 8.90625 3.08984C9.3194 3.11355 9.71076 3.28676 10.0049 3.58105C10.3219 3.89838 10.5002 4.32881 10.5 4.77734L10.4912 4.9502C10.4046 5.80271 9.685 6.46869 8.80957 6.46875H7.88281L7.87012 6.47363L6.47363 7.87109C6.47063 7.87424 6.46881 7.87837 6.46875 7.88281V8.80859C6.46875 9.74153 5.71333 10.4988 4.78027 10.5C3.87877 10.4998 3.1431 9.79412 3.09277 8.90527C2.67995 8.88133 2.289 8.70917 1.99512 8.41504C1.67807 8.09766 1.49973 7.66736 1.5 7.21875ZM6.53027 4.11719C6.52938 4.35177 6.44713 4.57782 6.29981 4.75781L6.23242 4.83203L4.83789 6.22949L4.83691 6.23047C4.64718 6.41966 4.39004 6.52706 4.12207 6.52832H3.19043C2.80944 6.52838 2.50019 6.83706 2.5 7.21875C2.49997 7.40196 2.57283 7.57843 2.70215 7.70801C2.83165 7.83765 3.00733 7.9111 3.19043 7.91113H3.58789C3.86403 7.91113 4.08789 8.13499 4.08789 8.41113V8.80859C4.08811 9.19059 4.39802 9.49982 4.7793 9.5L4.91797 9.48535C5.23219 9.42064 5.46875 9.14244 5.46875 8.80859V7.87988C5.4697 7.61156 5.57698 7.35396 5.7666 7.16406L7.16113 5.76855C7.35169 5.57699 7.61058 5.46936 7.88086 5.46875H8.80957C9.19065 5.46869 9.49996 5.15916 9.5 4.77734C9.50011 4.59396 9.42733 4.41775 9.29785 4.28809C9.16835 4.15844 8.99267 4.08499 8.80957 4.08496H8.41211C8.13597 4.08496 7.91211 3.8611 7.91211 3.58496V3.19141C7.91189 2.80941 7.60198 2.50018 7.2207 2.5C6.83954 2.5 6.53027 2.80952 6.53027 3.19141V4.11719Z"),
            )
        }.build()
        return _ic_bone_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBone12Preview() {
    Icon(
        imageVector = Icons.ic_bone_12,
        contentDescription = null,
    )
}