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

private var _ic_earth_16: ImageVector? = null

val Icons.ic_earth_16: ImageVector
    get() {
        if (_ic_earth_16 != null) return _ic_earth_16!!
        _ic_earth_16 = ImageVector.Builder(
            name = "ic_earth_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 1.99805C11.3216 1.99805 14.002 4.67845 14.002 8C14.0019 11.3215 11.3215 14.002 8 14.002C4.67849 14.002 1.99811 11.3215 1.99805 8C1.99805 4.67845 4.67845 1.99805 8 1.99805ZM8 3.24805C7.22193 3.24805 6.48896 3.43441 5.8418 3.76367L6.75195 4.67383C7.18568 5.10769 7.42958 5.69716 7.42969 6.31055C7.42942 7.58868 6.39343 8.625 5.11523 8.625H3.29004C3.59468 10.9589 5.58084 12.752 8 12.752C9.11021 12.752 10.1281 12.3725 10.9355 11.7383L10.1768 10.6016C10.1192 10.5155 10.0329 10.4543 9.93555 10.4297L9.83496 10.418L9.71777 10.4131C9.13512 10.372 8.6135 10.0265 8.34961 9.49902L8.27051 9.33984C8.03688 8.8725 8.03697 8.32187 8.27051 7.85449L8.94629 6.50098C9.22743 5.93775 9.80389 5.58301 10.4326 5.58301H12.0938C11.2682 4.18409 9.74733 3.24805 8 3.24805ZM10.4326 6.83301C10.296 6.83301 10.1703 6.90055 10.0947 7.00977L10.0654 7.05957L9.3877 8.41406C9.33037 8.52942 9.33011 8.66599 9.3877 8.78125L9.46777 8.94043C9.53774 9.07968 9.67972 9.16791 9.83496 9.16797C10.3907 9.16797 10.9087 9.44548 11.2168 9.90723L11.8262 10.8213C12.4075 10.0329 12.7519 9.05824 12.752 8C12.752 7.59701 12.7011 7.20626 12.6074 6.83301H10.4326ZM4.7959 4.48535C3.98849 5.22148 3.43897 6.2338 3.29004 7.375H5.11523C5.70307 7.375 6.17942 6.89832 6.17969 6.31055C6.17958 6.02831 6.06721 5.75679 5.86816 5.55762L4.7959 4.48535Z"),
            )
        }.build()
        return _ic_earth_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcEarth16Preview() {
    Icon(
        imageVector = Icons.ic_earth_16,
        contentDescription = null,
    )
}