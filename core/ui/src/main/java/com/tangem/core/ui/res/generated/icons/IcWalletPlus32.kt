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

private var _ic_wallet_plus_32: ImageVector? = null

val Icons.ic_wallet_plus_32: ImageVector
    get() {
        if (_ic_wallet_plus_32 != null) return _ic_wallet_plus_32!!
        _ic_wallet_plus_32 = ImageVector.Builder(
            name = "ic_wallet_plus_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.9033 5.00391C25.0287 5.11884 27.4965 7.66413 27.6074 10.7861C27.6085 10.819 27.6068 10.9406 27.6074 11.1025C28.8065 11.4991 29.4999 12.7346 29.4999 13.8848V18.124C29.4997 19.2739 28.8061 20.5077 27.6074 20.9043C27.6068 21.0606 27.6086 21.1808 27.6074 21.2148C27.4962 24.3363 25.0291 26.8808 21.9042 26.9961C21.79 27.0003 21.6606 27 21.4853 27H17.2646C16.4363 27 15.7649 26.3282 15.7646 25.5C15.7648 24.6718 16.4363 24 17.2646 24H21.4853C21.6872 24 21.7486 23.9997 21.7939 23.9981C23.3024 23.9423 24.5523 22.703 24.6093 21.1084C24.6093 21.1033 24.609 21.0803 24.6093 21.0371H23.7333C21.0217 21.0371 19.1857 18.5677 19.1855 16.0039C19.1857 13.4401 21.0216 10.9717 23.7333 10.9717H24.6093C24.609 10.9242 24.6093 10.899 24.6093 10.8926C24.5527 9.29737 23.3019 8.05745 21.7929 8.00196C21.7476 8.0003 21.6871 8 21.4853 8H11.0761C9.81258 8 8.95671 8.0017 8.29584 8.05665C7.65343 8.11007 7.3294 8.20614 7.10541 8.32227C6.55756 8.6064 6.10701 9.06289 5.82318 9.62989C5.70452 9.86701 5.60857 10.2065 5.5556 10.8662C5.50128 11.5429 5.49994 12.4182 5.49994 13.7031C5.49985 14.5315 4.82831 15.2031 3.99994 15.2031C3.17157 15.2031 2.50003 14.5315 2.49994 13.7031C2.49994 12.4669 2.49918 11.4506 2.56537 10.626C2.63291 9.78486 2.77738 9.01276 3.14056 8.28711C3.70476 7.16004 4.60807 6.23823 5.72455 5.65918C6.44642 5.28489 7.21468 5.13563 8.04681 5.06641C8.86087 4.99871 9.86286 5 11.0761 5H21.4853C21.6607 5 21.789 4.99971 21.9033 5.00391ZM23.7333 13.9717C23.0788 13.9717 22.1857 14.6661 22.1855 16.0039C22.1857 17.3416 23.0788 18.0371 23.7333 18.0371H26.4999V13.9717H23.7333Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.04877 15.8516C8.87717 15.8516 9.54877 16.5232 9.54877 17.3516V19.8252H11.9511C12.7794 19.8252 13.451 20.4969 13.4511 21.3252C13.4511 22.1536 12.7795 22.8252 11.9511 22.8252H9.54877V25.2979C9.54862 26.1261 8.87708 26.7978 8.04877 26.7979C7.22051 26.7978 6.54892 26.1261 6.54877 25.2979V22.8252H4.14642C3.318 22.8252 2.64642 22.1536 2.64642 21.3252C2.64656 20.4969 3.31808 19.8252 4.14642 19.8252H6.54877V17.3516C6.54877 16.5232 7.22042 15.8517 8.04877 15.8516Z"),
            )
        }.build()
        return _ic_wallet_plus_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletPlus32Preview() {
    Icon(
        imageVector = Icons.ic_wallet_plus_32,
        contentDescription = null,
    )
}