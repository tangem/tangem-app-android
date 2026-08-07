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

private var _ic_binoculars_32: ImageVector? = null

val Icons.ic_binoculars_32: ImageVector
    get() {
        if (_ic_binoculars_32 != null) return _ic_binoculars_32!!
        _ic_binoculars_32 = ImageVector.Builder(
            name = "ic_binoculars_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.89424 6C12.0387 6.00016 13.8678 7.45751 14.362 9.45215H17.6306C18.1248 7.4579 19.9531 6.00129 22.0974 6.00098C24.176 6.00066 26.0186 7.37886 26.5495 9.39062L28.7116 17.5684C28.8987 18.1282 29.0006 18.7227 29.0007 19.335C29.0007 21.6402 27.5827 23.7003 25.4392 24.5693C23.2989 25.4366 20.8323 24.9573 19.1872 23.3418C18.2353 22.4068 17.6868 21.2255 17.5397 20.0059H14.4616C14.4005 20.5161 14.2692 21.0241 14.0622 21.5146C13.1663 23.6372 11.0628 25.0027 8.75068 25.0029C6.43858 25.0029 4.33621 23.637 3.44014 21.5146C2.92702 20.2989 2.87216 18.9796 3.22725 17.7705L5.44307 9.39062C5.97408 7.37893 7.81563 5.99967 9.89424 6ZM11.0563 17.1143C9.78539 15.8718 7.71717 15.8721 6.446 17.1143C5.52053 18.0191 5.24877 19.3708 5.74287 20.542C6.23918 21.7174 7.42052 22.5029 8.75068 22.5029C10.0808 22.5027 11.2623 21.7175 11.7585 20.542C11.9323 20.1299 12.0095 19.6951 11.9987 19.2656H11.9948V19.1475C11.9472 18.398 11.626 17.6714 11.0563 17.1143ZM24.5007 16.418C23.275 15.9213 21.867 16.2016 20.9392 17.1123C19.6853 18.3439 19.6852 20.327 20.9392 21.5586C21.867 22.4697 23.2748 22.7487 24.5007 22.252C25.7223 21.7563 26.5007 20.5984 26.5007 19.335C26.5006 19.0741 26.4645 18.8182 26.4011 18.5713L26.3923 18.5742L26.3249 18.3193C26.0314 17.4804 25.383 16.7757 24.5007 16.418ZM14.4948 17.5059H17.4978V11.9521H14.4948V17.5059ZM9.89424 8.5C8.91962 8.49978 8.09347 9.14465 7.86006 10.0293L6.80537 14.0146C8.51382 13.4118 10.4539 13.6303 11.9948 14.6689V10.5332C11.9947 9.50323 11.189 8.61753 10.113 8.51074L9.89424 8.5ZM22.0983 8.5C20.9144 8.5 19.9979 9.43444 19.9978 10.5332V14.666C21.507 13.6479 23.4367 13.3915 25.1833 14.0059L24.1325 10.0303C23.9137 9.20092 23.1736 8.5817 22.279 8.50781L22.0983 8.5Z"),
            )
        }.build()
        return _ic_binoculars_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBinoculars32Preview() {
    Icon(
        imageVector = Icons.ic_binoculars_32,
        contentDescription = null,
    )
}