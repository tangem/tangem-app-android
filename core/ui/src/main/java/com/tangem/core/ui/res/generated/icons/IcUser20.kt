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

private var _ic_user_20: ImageVector? = null

val Icons.ic_user_20: ImageVector
    get() {
        if (_ic_user_20 != null) return _ic_user_20!!
        _ic_user_20 = ImageVector.Builder(
            name = "ic_user_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.293 12.4866C14.9657 12.4867 16.9988 14.6312 16.999 17.1916C16.9988 17.6056 16.663 17.9415 16.249 17.9416C15.835 17.9416 15.4992 17.6056 15.499 17.1916C15.4988 15.4375 14.1153 13.9867 12.293 13.9866H7.70703C5.95231 13.9867 4.50123 15.3699 4.50098 17.1916C4.50077 17.6057 4.16507 17.9416 3.75098 17.9416C3.33694 17.9416 3.00118 17.6056 3.00098 17.1916C3.00124 14.5191 5.14645 12.4867 7.70703 12.4866H12.293Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.77148 3.34105C8.58868 1.63103 11.3771 1.63119 13.1943 3.34105C13.1981 3.3446 13.2014 3.34917 13.2051 3.35277C13.2068 3.35445 13.2092 3.35596 13.2109 3.35766C15.0229 5.16963 15.0465 8.08429 13.1943 9.82738C11.4081 11.5081 8.50133 11.667 6.73926 9.79516C5.05851 8.00917 4.8995 5.10285 6.77148 3.34105ZM12.1543 4.42309C10.9144 3.26758 9.03559 3.27092 7.7998 4.43383C6.61528 5.5487 6.63608 7.49731 7.83203 8.76781C8.9471 9.95208 10.8963 9.93122 12.167 8.73559C13.37 7.60337 13.3943 5.6678 12.1543 4.42309Z"),
            )
        }.build()
        return _ic_user_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser20Preview() {
    Icon(
        imageVector = Icons.ic_user_20,
        contentDescription = null,
    )
}