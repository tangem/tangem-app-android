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

private var _ic_wallet_checkmark_32: ImageVector? = null

val Icons.ic_wallet_checkmark_32: ImageVector
    get() {
        if (_ic_wallet_checkmark_32 != null) return _ic_wallet_checkmark_32!!
        _ic_wallet_checkmark_32 = ImageVector.Builder(
            name = "ic_wallet_checkmark_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.9033 5.00342C25.0286 5.11835 27.4965 7.66367 27.6074 10.7856C27.6085 10.8187 27.6068 10.9403 27.6074 11.1021C28.8065 11.4986 29.4999 12.7341 29.4999 13.8843V18.1235C29.4996 19.2734 28.806 20.5072 27.6074 20.9038C27.6068 21.0604 27.6086 21.1802 27.6074 21.2144C27.4964 24.3359 25.0292 26.8803 21.9042 26.9956C21.79 26.9998 21.6606 26.9995 21.4853 26.9995H17.2646C16.4362 26.9995 15.7646 26.3279 15.7646 25.4995C15.7647 24.6712 16.4363 23.9995 17.2646 23.9995H21.4853C21.6872 23.9995 21.7486 23.9992 21.7939 23.9976C23.3025 23.9418 24.5525 22.7027 24.6093 21.1079C24.6093 21.1028 24.609 21.0803 24.6093 21.0366H23.7333C21.0217 21.0366 19.1857 18.5672 19.1855 16.0034C19.1856 13.4396 21.0216 10.9712 23.7333 10.9712H24.6093C24.609 10.9239 24.6093 10.8987 24.6093 10.8921C24.5527 9.29697 23.3019 8.05696 21.7929 8.00147C21.7476 7.99981 21.6871 7.99952 21.4853 7.99952H11.0761C9.81258 7.99952 8.95672 8.00121 8.29584 8.05616C7.65343 8.10958 7.3294 8.20566 7.10541 8.32178C6.55756 8.60591 6.10701 9.06242 5.82318 9.6294C5.70453 9.86651 5.60856 10.206 5.5556 10.8657C5.50128 11.5424 5.49994 12.4178 5.49994 13.7026C5.49975 14.5309 4.82825 15.2026 3.99994 15.2026C3.17163 15.2026 2.50013 14.5309 2.49994 13.7026C2.49994 12.4664 2.49918 11.4501 2.56537 10.6255C2.6329 9.78437 2.77738 9.01228 3.14056 8.28663C3.70476 7.15957 4.60807 6.23772 5.72455 5.6587C6.44643 5.28441 7.21468 5.13514 8.04681 5.06592C8.86087 4.99822 9.86285 4.99952 11.0761 4.99952H21.4853C21.6607 4.99952 21.789 4.99922 21.9033 5.00342ZM23.7333 13.9712C23.0787 13.9712 22.1856 14.6657 22.1855 16.0034C22.1857 17.3411 23.0787 18.0366 23.7333 18.0366H26.4999V13.9712H23.7333Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.0839 18.5786C12.6643 17.9878 13.615 17.979 14.206 18.5591C14.7969 19.1396 14.8049 20.0892 14.2245 20.6802L8.65424 26.3511C8.3722 26.6382 7.9864 26.8003 7.58392 26.8003C7.18176 26.8001 6.79643 26.638 6.51459 26.3511L3.17181 22.9487C2.59191 22.3577 2.60057 21.408 3.19135 20.8276C3.78218 20.2475 4.73193 20.2558 5.31244 20.8462L7.58392 23.1597L12.0839 18.5786Z"),
            )
        }.build()
        return _ic_wallet_checkmark_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletCheckmark32Preview() {
    Icon(
        imageVector = Icons.ic_wallet_checkmark_32,
        contentDescription = null,
    )
}