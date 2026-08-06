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

private var _ic_success_32: ImageVector? = null

val Icons.ic_success_32: ImageVector
    get() {
        if (_ic_success_32 != null) return _ic_success_32!!
        _ic_success_32 = ImageVector.Builder(
            name = "ic_success_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 2C23.732 2 30 8.26801 30 16C30 23.732 23.732 30 16 30C8.26801 30 2 23.732 2 16C2 8.26801 8.26801 2 16 2ZM16 5C9.92487 5 5 9.92487 5 16C5 22.0751 9.92487 27 16 27C22.0751 27 27 22.0751 27 16C27 9.92487 22.0751 5 16 5ZM19.7822 11.6064C20.368 11.0207 21.3185 11.0207 21.9043 11.6064C22.4896 12.1922 22.4898 13.1419 21.9043 13.7275L15.084 20.5469C14.8027 20.8281 14.4212 20.9863 14.0234 20.9863C13.6257 20.9863 13.2442 20.8282 12.9629 20.5469L10.0186 17.6025C9.43294 17.0167 9.43286 16.0672 10.0186 15.4814C10.6043 14.8958 11.5539 14.8959 12.1396 15.4814L14.0234 17.3652L19.7822 11.6064Z"),
            )
        }.build()
        return _ic_success_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSuccess32Preview() {
    Icon(
        imageVector = Icons.ic_success_32,
        contentDescription = null,
    )
}