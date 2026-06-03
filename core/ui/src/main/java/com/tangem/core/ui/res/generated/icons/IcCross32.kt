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

private var _ic_cross_32: ImageVector? = null

val Icons.ic_cross_32: ImageVector
    get() {
        if (_ic_cross_32 != null) return _ic_cross_32!!
        _ic_cross_32 = ImageVector.Builder(
            name = "ic_cross_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23.4394 6.44093C24.0252 5.85538 24.9748 5.85523 25.5605 6.44093C26.1455 7.02668 26.1458 7.97644 25.5605 8.56202L18.122 16.0005L25.5605 23.4399C26.1455 24.0257 26.1457 24.9755 25.5605 25.561C24.9749 26.1465 24.0252 26.1461 23.4394 25.561L16 18.1226L8.56148 25.561C7.97589 26.1465 7.02619 26.1462 6.44038 25.561C5.8548 24.9753 5.85476 24.0257 6.44038 23.4399L13.8789 16.0005L6.44038 8.56202C5.85464 7.97627 5.8547 7.02672 6.44038 6.44093C7.02619 5.85545 7.97579 5.85524 8.56148 6.44093L16 13.8794L23.4394 6.44093Z"),
            )
        }.build()
        return _ic_cross_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCross32Preview() {
    Icon(
        imageVector = Icons.ic_cross_32,
        contentDescription = null,
    )
}