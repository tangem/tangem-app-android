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

private var _ic_clapper_board_32: ImageVector? = null

val Icons.ic_clapper_board_32: ImageVector
    get() {
        if (_ic_clapper_board_32 != null) return _ic_clapper_board_32!!
        _ic_clapper_board_32 = ImageVector.Builder(
            name = "ic_clapper_board_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23.412 3.03628C24.0504 2.93067 24.7075 3.05647 25.2655 3.39273C25.9018 3.77615 26.3517 4.39811 26.5263 5.11148L27.206 7.88882C27.5583 9.32987 26.7216 10.8357 25.2528 11.2365L17.9696 13.2218H24.7489C26.2984 13.2218 27.4985 14.4967 27.4989 15.9992V24.9445C27.4988 27.1529 25.7389 29.0001 23.4989 29.0001H9.26359C7.02373 29 5.26374 27.1528 5.26359 24.9445V14.9025L4.61808 12.2628C4.10017 10.1445 5.33298 7.95059 7.46183 7.37027L23.1405 3.09585L23.412 3.03628ZM8.26359 24.9445C8.26373 25.5585 8.74246 26 9.26359 26.0001H23.4989C24.0202 26.0001 24.4988 25.5586 24.4989 24.9445V16.2218H8.26359V24.9445ZM8.25089 10.2648C7.74123 10.4037 7.38812 10.9602 7.53214 11.55L7.85441 12.8693L10.9423 12.0275V9.53042L8.25089 10.2648ZM13.9423 8.71206V11.2091L17.37 10.2755V7.77749L13.9423 8.71206ZM20.37 6.96011V9.45718L24.2431 8.40249L23.6698 6.06069L20.37 6.96011Z"),
            )
        }.build()
        return _ic_clapper_board_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClapperBoard32Preview() {
    Icon(
        imageVector = Icons.ic_clapper_board_32,
        contentDescription = null,
    )
}