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

private var _ic_perps_24: ImageVector? = null

val Icons.ic_perps_24: ImageVector
    get() {
        if (_ic_perps_24 != null) return _ic_perps_24!!
        _ic_perps_24 = ImageVector.Builder(
            name = "ic_perps_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.90039 9.82715C8.06245 8.66548 9.92332 8.62948 11.1289 9.71875L11.2441 9.82715L12 10.585L12.7568 9.8291C13.956 8.63001 15.9007 8.63007 17.0996 9.83008C18.2987 11.0296 18.2999 12.9734 17.1006 14.1729C15.9012 15.372 13.9563 15.3719 12.7568 14.1729L12 13.4141L11.2441 14.1709C10.0451 15.37 8.10028 15.3697 6.90137 14.1699C5.70184 12.9704 5.70087 11.0267 6.90039 9.82715ZM15.6855 11.2432C15.2684 10.8255 14.5896 10.8245 14.1709 11.2432L13.4131 12.001L14.1709 12.7588C14.5892 13.1767 15.2671 13.1765 15.6855 12.7588C16.0777 12.3667 16.1025 11.746 15.7598 11.3252L15.6855 11.2432ZM9.8291 11.2412C9.41071 10.8233 8.73281 10.8233 8.31445 11.2412C7.89598 11.6597 7.89598 12.3384 8.31445 12.7568C8.73161 13.1743 9.41143 13.1755 9.83008 12.7568L10.5859 11.999L9.8291 11.2412Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2ZM12 4C7.58172 4 4 7.58172 4 12C4 16.4183 7.58172 20 12 20C16.4183 20 20 16.4183 20 12C20 7.58172 16.4183 4 12 4Z"),
            )
        }.build()
        return _ic_perps_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPerps24Preview() {
    Icon(
        imageVector = Icons.ic_perps_24,
        contentDescription = null,
    )
}