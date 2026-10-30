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

private var _ic_lock_32: ImageVector? = null

val Icons.ic_lock_32: ImageVector
    get() {
        if (_ic_lock_32 != null) return _ic_lock_32!!
        _ic_lock_32 = ImageVector.Builder(
            name = "ic_lock_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 2.5C19.801 2.50007 22.9285 5.53453 22.9287 9.33301V10.5029C25.1672 10.5772 27 12.3887 27 14.667V25.333C27 27.6595 25.0889 29.4998 22.7861 29.5H9.21387C6.91112 29.4998 5 27.6595 5 25.333V14.667C5 12.3887 6.83278 10.5772 9.07129 10.5029V9.33301C9.07147 5.53448 12.1989 2.5 16 2.5ZM9.21387 13.5C8.51772 13.5002 8 14.0471 8 14.667V25.333C8 25.9529 8.51772 26.4998 9.21387 26.5H22.7861C23.4823 26.4998 24 25.9529 24 25.333V14.667C24 14.0471 23.4823 13.5002 22.7861 13.5H9.21387ZM16 5.5C13.8055 5.5 12.0715 7.24117 12.0713 9.33301V10.5H19.9287V9.33301C19.9285 7.24121 18.1945 5.50007 16 5.5Z"),
            )
        }.build()
        return _ic_lock_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLock32Preview() {
    Icon(
        imageVector = Icons.ic_lock_32,
        contentDescription = null,
    )
}