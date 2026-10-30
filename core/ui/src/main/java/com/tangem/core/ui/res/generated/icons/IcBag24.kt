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

private var _ic_bag_24: ImageVector? = null

val Icons.ic_bag_24: ImageVector
    get() {
        if (_ic_bag_24 != null) return _ic_bag_24!!
        _ic_bag_24 = ImageVector.Builder(
            name = "ic_bag_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.917 9.05566C15.4691 9.05593 15.917 9.50354 15.917 10.0557V11.0283C15.9168 13.1235 14.2713 14.8343 12.2021 14.9395L12.001 14.9443L11.7988 14.9395C9.72965 14.8344 8.08421 13.1235 8.08398 11.0283V10.0557C8.08398 9.50338 8.5317 9.05566 9.08398 9.05566C9.63604 9.05593 10.084 9.50354 10.084 10.0557V11.0283C10.0842 12.0867 10.9426 12.9443 12.001 12.9443C13.059 12.9439 13.9168 12.0864 13.917 11.0283V10.0557C13.917 9.50349 14.3649 9.05584 14.917 9.05566Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.834 2.25C19.4598 2.25038 20.7783 3.56845 20.7783 5.19434V5.92969C21.3738 6.46845 21.75 7.24506 21.75 8.11133V18.8057C21.75 20.4318 20.4318 21.7499 18.8057 21.75H5.19434C3.56821 21.7499 2.25 20.4318 2.25 18.8057V8.11133C2.25003 7.24457 2.62659 6.46751 3.22266 5.92871V5.19434C3.22271 3.56825 4.54091 2.25006 6.16699 2.25H17.834ZM5.19434 7.16699C4.67282 7.16705 4.25006 7.58981 4.25 8.11133V18.8057C4.25 19.3272 4.67278 19.7499 5.19434 19.75H18.8057C19.3272 19.7499 19.75 19.3272 19.75 18.8057V8.11133C19.7499 7.58981 19.3272 7.16705 18.8057 7.16699H5.19434ZM6.16699 4.25C5.65463 4.25006 5.23817 4.65814 5.22363 5.16699H18.7764C18.7618 4.65833 18.3461 4.25037 17.834 4.25H6.16699Z"),
            )
        }.build()
        return _ic_bag_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBag24Preview() {
    Icon(
        imageVector = Icons.ic_bag_24,
        contentDescription = null,
    )
}