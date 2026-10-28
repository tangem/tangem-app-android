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

private var _ic_bone_32: ImageVector? = null

val Icons.ic_bone_32: ImageVector
    get() {
        if (_ic_bone_32 != null) return _ic_bone_32!!
        _ic_bone_32 = ImageVector.Builder(
            name = "ic_bone_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.5859 7.9248C14.5859 5.2058 16.7894 3 19.5088 3C22.1098 3.00015 24.2369 5.01697 24.4189 7.57129C25.6003 7.65352 26.7172 8.1587 27.5596 9.00195C28.4824 9.92586 29.0005 11.1786 29 12.4844L28.9932 12.7383C28.8612 15.3394 26.7115 17.4092 24.0771 17.4092H21.418L17.4102 21.418V24.0752C17.4102 26.7919 15.2102 28.9967 12.4932 29L12.4912 27.5V29C9.88624 28.9999 7.75542 26.9769 7.5791 24.417C6.39843 24.3343 5.28232 23.8301 4.44043 22.9873C3.51738 22.0631 2.9992 20.8101 3 19.5039C3.00052 16.7853 5.20374 14.5801 7.92285 14.5801H10.585L14.5859 10.5703V7.9248ZM17.5859 10.5859C17.5832 11.3736 17.2694 12.129 16.7129 12.6865L12.6992 16.708C12.1421 17.2633 11.3882 17.5764 10.6016 17.5801H7.92285C6.86156 17.5801 6 18.4414 6 19.5049C5.99971 20.0156 6.20279 20.5061 6.56348 20.8672C6.92415 21.2282 7.41295 21.4316 7.92285 21.4316H9.06641C9.89483 21.4316 10.5664 22.1032 10.5664 22.9316V24.0732C10.5664 25.1374 11.4283 25.9984 12.4902 25.999C13.5503 25.9974 14.4102 25.1375 14.4102 24.0752V21.4033C14.4129 20.6154 14.7274 19.8594 15.2842 19.3018L19.2939 15.29C19.8537 14.7275 20.6143 14.4107 21.4082 14.4092H24.0771C25.1384 14.4092 26 13.5478 26 12.4844C26.0002 11.9737 25.7972 11.4831 25.4365 11.1221C25.0759 10.7611 24.587 10.5586 24.0771 10.5586H22.9336C22.1053 10.5586 21.4338 9.88682 21.4336 9.05859V7.92676C21.4336 6.86226 20.5711 6.00016 19.5088 6C18.4475 6 17.5859 6.86137 17.5859 7.9248V10.5859Z"),
            )
        }.build()
        return _ic_bone_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBone32Preview() {
    Icon(
        imageVector = Icons.ic_bone_32,
        contentDescription = null,
    )
}