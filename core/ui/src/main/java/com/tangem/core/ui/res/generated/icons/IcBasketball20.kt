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

private var _ic_basketball_20: ImageVector? = null

val Icons.ic_basketball_20: ImageVector
    get() {
        if (_ic_basketball_20 != null) return _ic_basketball_20!!
        _ic_basketball_20 = ImageVector.Builder(
            name = "ic_basketball_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.34137 4.34528C7.46431 1.22116 12.5288 1.22151 15.6519 4.34528L15.9351 4.64411C17.2965 6.15139 17.9807 8.05261 17.9918 9.95661C17.9927 9.97183 17.9947 9.98706 17.9947 10.0025C17.9947 10.0191 17.9928 10.0359 17.9918 10.0523C17.9794 12.0833 17.2009 14.1102 15.6519 15.6597C12.5288 18.7839 7.46443 18.784 4.34137 15.6597C1.21882 12.5355 1.21856 7.46931 4.34137 4.34528ZM5.96442 15.0972C6.94694 15.8777 8.10178 16.3328 9.28668 16.4625C9.44645 14.8326 10.0946 13.346 11.0836 12.1519L9.99567 11.064L5.96442 15.0972ZM12.1509 13.2193C11.4295 14.1301 10.9475 15.2385 10.7984 16.4508C11.951 16.3082 13.0718 15.8581 14.0289 15.0982L12.1509 13.2193ZM7.84918 8.91559C6.65538 9.90519 5.16943 10.5537 3.53961 10.7134C3.66912 11.8986 4.12433 13.0539 4.90387 14.0367L8.9361 10.0025L7.84918 8.91559ZM16.4429 10.8033C15.2307 10.9526 14.1221 11.4353 13.2115 12.1578L15.0894 14.0367C15.8494 13.0787 16.3006 11.9571 16.4429 10.8033ZM11.0572 10.0025L12.1441 11.0904C13.3377 10.1006 14.824 9.45171 16.4537 9.29157C16.324 8.10638 15.8692 6.95109 15.0894 5.96833L11.0572 10.0025ZM4.90387 5.96833C4.14399 6.92616 3.69274 8.04816 3.55035 9.20173C4.76295 9.05268 5.87085 8.56977 6.7818 7.84724L4.90387 5.96833ZM10.7076 3.54255C10.5479 5.17315 9.89924 6.65964 8.90973 7.85407L9.99664 8.94099L14.0289 4.90681C13.0466 4.12706 11.8919 3.67217 10.7076 3.54255ZM9.19586 3.55329C8.04292 3.69582 6.92176 4.14746 5.96442 4.90778L7.84235 6.78669C8.56457 5.8753 9.04693 4.76661 9.19586 3.55329Z"),
            )
        }.build()
        return _ic_basketball_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBasketball20Preview() {
    Icon(
        imageVector = Icons.ic_basketball_20,
        contentDescription = null,
    )
}