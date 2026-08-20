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

private var _ic_users_24: ImageVector? = null

val Icons.ic_users_24: ImageVector
    get() {
        if (_ic_users_24 != null) return _ic_users_24!!
        _ic_users_24 = ImageVector.Builder(
            name = "ic_users_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.3867 12.9121C12.6562 12.9123 14.5595 14.6941 14.5596 16.9697V17.751C14.5591 18.3028 14.1114 18.7509 13.5596 18.751C13.0076 18.751 12.5601 18.3028 12.5596 17.751V16.9697C12.5595 15.8678 11.622 14.9123 10.3867 14.9121H7.17285C5.93746 14.9121 5.00004 15.8677 5 16.9697V17.751C4.9995 18.3028 4.55198 18.751 4 18.751C3.44802 18.751 3.0005 18.3028 3 17.751V16.9697C3.00004 14.694 4.90325 12.9121 7.17285 12.9121H10.3867Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.8271 12.9121C19.0967 12.9121 21 14.6941 21 16.9697V17.751C20.9995 18.3028 20.552 18.751 20 18.751C19.448 18.751 19.0005 18.3028 19 17.751V16.9697C19 15.8677 18.0625 14.9121 16.8271 14.9121H15.9492C15.3971 14.9121 14.9495 14.4642 14.9492 13.9121C14.9492 13.3598 15.3969 12.9121 15.9492 12.9121H16.8271Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.9492 6.77051C17.5659 6.77076 18.9413 8.04297 18.9414 9.68945C18.9411 11.3358 17.5658 12.6091 15.9492 12.6094C14.3325 12.6092 12.9583 11.3359 12.958 9.68945C12.9582 8.04291 14.3325 6.77068 15.9492 6.77051ZM15.9492 8.77051C15.3667 8.77067 14.9582 9.21651 14.958 9.68945C14.9583 10.1623 15.3668 10.6092 15.9492 10.6094C16.5316 10.6091 16.9411 10.1623 16.9414 9.68945C16.9413 9.21656 16.5316 8.77076 15.9492 8.77051Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.7793 5.25C10.8316 5.25031 12.5596 6.86336 12.5596 8.92969C12.5593 10.9958 10.8314 12.6081 8.7793 12.6084C6.7269 12.6084 4.99928 10.996 4.99902 8.92969C4.99902 6.86317 6.72675 5.25 8.7793 5.25ZM8.7793 7.25C7.76095 7.25 6.99902 8.03682 6.99902 8.92969C6.99929 9.82236 7.76112 10.6084 8.7793 10.6084C9.79718 10.6081 10.5593 9.82219 10.5596 8.92969C10.5596 8.03699 9.79735 7.2503 8.7793 7.25Z"),
            )
        }.build()
        return _ic_users_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUsers24Preview() {
    Icon(
        imageVector = Icons.ic_users_24,
        contentDescription = null,
    )
}