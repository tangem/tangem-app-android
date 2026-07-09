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

private var _ic_cloud_exclamation_32: ImageVector? = null

val Icons.ic_cloud_exclamation_32: ImageVector
    get() {
        if (_ic_cloud_exclamation_32 != null) return _ic_cloud_exclamation_32!!
        _ic_cloud_exclamation_32 = ImageVector.Builder(
            name = "ic_cloud_exclamation_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.0191 22.9534C16.6496 22.9604 17.2211 23.3164 17.5057 23.8723L17.5595 23.9875L17.6045 24.1067C17.8057 24.7082 17.6522 25.3763 17.1996 25.8313C16.7167 26.3165 15.9883 26.4632 15.355 26.2024C14.722 25.9416 14.3087 25.3256 14.3085 24.6418V24.634C14.3115 23.7433 15.0057 23.0187 15.8821 22.9592C15.9176 22.9565 15.9536 22.9526 15.9897 22.9524C15.9923 22.9524 15.9949 22.9534 15.9976 22.9534L16.0034 22.9524C16.0086 22.9524 16.0139 22.9533 16.0191 22.9534Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.0005 5.67017C20.5125 5.67047 24.2482 8.9868 24.8996 13.3108C27.8167 13.9506 30 16.5458 30 19.6506C29.9995 23.2353 27.0892 26.1416 23.499 26.1418H20.9992C20.1697 26.1418 19.4972 25.47 19.4969 24.6418C19.497 23.8135 20.1696 23.1419 20.9992 23.1418H23.499C25.4298 23.1416 26.995 21.5785 26.9955 19.6506C26.9955 17.7224 25.4301 16.1587 23.499 16.1584C22.6695 16.1584 21.9971 15.4866 21.9968 14.6584C21.9967 11.4551 19.4768 8.83938 16.3086 8.67896L16.0005 8.67114C13.4345 8.67255 11.1545 10.3048 10.3289 12.7307C10.1452 13.2699 9.67075 13.659 9.10538 13.7346C6.76077 14.0478 5.00873 16.0436 5.00451 18.4055C5.00648 21.0213 7.13072 23.1417 9.75088 23.1418H11.0008C11.8303 23.142 12.503 23.8135 12.5031 24.6418C12.5028 25.47 11.8302 26.1417 11.0008 26.1418H9.75088C5.4704 26.1417 2.00039 22.6766 2 18.4026V18.3997L2.0088 18.0393C2.17223 14.6385 4.55082 11.7664 7.81438 10.9348C9.26176 7.75593 12.449 5.67189 16.0005 5.67017Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.0005 14.7415C16.8299 14.7417 17.5027 15.4132 17.5027 16.2415V20.0178C17.5027 20.8461 16.8299 21.5176 16.0005 21.5178C15.1708 21.5178 14.4982 20.8462 14.4982 20.0178V16.2415C14.4982 15.413 15.1708 14.7415 16.0005 14.7415Z"),
            )
        }.build()
        return _ic_cloud_exclamation_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloudExclamation32Preview() {
    Icon(
        imageVector = Icons.ic_cloud_exclamation_32,
        contentDescription = null,
    )
}