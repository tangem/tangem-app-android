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

private var _ic_card_32: ImageVector? = null

val Icons.ic_card_32: ImageVector
    get() {
        if (_ic_card_32 != null) return _ic_card_32!!
        _ic_card_32 = ImageVector.Builder(
            name = "ic_card_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7598 18.5713C13.5881 18.5713 14.2596 19.243 14.2598 20.0713C14.2597 20.8996 13.5881 21.5712 12.7598 21.5713H9.33301C8.50474 21.5712 7.83308 20.8996 7.83301 20.0713C7.83314 19.2431 8.50477 18.5714 9.33301 18.5713H12.7598Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24 5C27.0626 5 29.4999 7.51928 29.5 10.5713V21.4287C29.4999 24.4807 27.0626 27 24 27H8C4.93735 27 2.50006 24.4807 2.5 21.4287V10.5713C2.50008 7.51928 4.93737 5 8 5H24ZM5.5 21.4287C5.50006 22.8742 6.64405 24 8 24H24C25.356 24 26.4999 22.8742 26.5 21.4287V13.4287H5.5V21.4287ZM8 8C6.68903 8 5.57928 9.05257 5.50684 10.4287H26.4932C26.4207 9.05257 25.311 8 24 8H8Z"),
            )
        }.build()
        return _ic_card_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCard32Preview() {
    Icon(
        imageVector = Icons.ic_card_32,
        contentDescription = null,
    )
}