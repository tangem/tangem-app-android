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

private var _ic_user_20_filled: ImageVector? = null

val Icons.ic_user_20_filled: ImageVector
    get() {
        if (_ic_user_20_filled != null) return _ic_user_20_filled!!
        _ic_user_20_filled = ImageVector.Builder(
            name = "ic_user_20_filled",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.77148 3.34105C8.58868 1.63103 11.3771 1.63119 13.1943 3.34105C13.1981 3.3446 13.2014 3.34917 13.2051 3.35277C13.2068 3.35445 13.2092 3.35596 13.2109 3.35766C15.0229 5.16963 15.0465 8.08429 13.1943 9.82738C11.4081 11.5081 8.50133 11.667 6.73926 9.79516C5.05851 8.00917 4.8995 5.10285 6.77148 3.34105Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.293 12.4866C14.9657 12.4867 16.9988 14.6312 16.999 17.1916C16.9988 17.6056 16.663 17.9415 16.249 17.9416H3.75098C3.33694 17.9416 3.00118 17.6056 3.00098 17.1916C3.00124 14.5191 5.14645 12.4867 7.70703 12.4866H12.293Z"),
            )
        }.build()
        return _ic_user_20_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser20FilledPreview() {
    Icon(
        imageVector = Icons.ic_user_20_filled,
        contentDescription = null,
    )
}