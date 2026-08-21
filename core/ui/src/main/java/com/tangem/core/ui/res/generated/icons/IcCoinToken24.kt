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

private var _ic_coin_token_24: ImageVector? = null

val Icons.ic_coin_token_24: ImageVector
    get() {
        if (_ic_coin_token_24 != null) return _ic_coin_token_24!!
        _ic_coin_token_24 = ImageVector.Builder(
            name = "ic_coin_token_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.58967 8.59277C8.98021 8.20269 9.61334 8.20246 10.0037 8.59277L12.703 11.293C13.0931 11.6835 13.0934 12.3166 12.703 12.707L10.0037 15.4062C9.61332 15.7967 8.98022 15.7964 8.58967 15.4062L5.88948 12.707C5.49919 12.3167 5.49946 11.6835 5.88948 11.293L8.58967 8.59277ZM8.01155 12L9.2967 13.2852L10.5819 12L9.2967 10.7148L8.01155 12Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.6971 4.7002C18.7283 4.70055 21.9969 7.9687 21.9969 12C21.9968 16.0313 18.7283 19.2994 14.6971 19.2998C13.7431 19.2998 12.8361 19.1092 12.0057 18.7773C9.37701 19.8271 6.26197 19.2895 4.13362 17.1611C1.28339 14.3104 1.28313 9.68846 4.13362 6.83789C6.262 4.70951 9.37785 4.17082 12.0067 5.2207C12.8367 4.88927 13.7437 4.7002 14.6971 4.7002ZM14.6971 6.7002C14.5728 6.7002 14.4492 6.70518 14.327 6.71387C14.3702 6.75505 14.4144 6.79544 14.4569 6.83789C17.3076 9.6886 17.3076 14.3104 14.4569 17.1611C14.4144 17.2036 14.3692 17.243 14.326 17.2842C14.4486 17.2929 14.5724 17.2998 14.6971 17.2998C17.6237 17.2994 19.9968 14.9267 19.9969 12C19.9969 9.07327 17.6237 6.70055 14.6971 6.7002ZM11.4823 7.17188C9.53035 6.28844 7.15331 6.64828 5.54866 8.25293C3.47909 10.3226 3.47909 13.6774 5.54866 15.7471C7.15233 17.3507 9.52723 17.7106 11.4783 16.8291C11.5363 16.7942 11.5974 16.7647 11.661 16.7422C12.1593 16.4935 12.6272 16.1626 13.0428 15.7471C15.0477 13.7421 15.1108 10.5306 13.2313 8.4502L13.0428 8.25293L12.8455 8.06445C12.4794 7.73364 12.0776 7.46436 11.6541 7.25391C11.5944 7.23215 11.537 7.20462 11.4823 7.17188Z"),
            )
        }.build()
        return _ic_coin_token_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCoinToken24Preview() {
    Icon(
        imageVector = Icons.ic_coin_token_24,
        contentDescription = null,
    )
}