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

private var _ic_rose_28: ImageVector? = null

val Icons.ic_rose_28: ImageVector
    get() {
        if (_ic_rose_28 != null) return _ic_rose_28!!
        _ic_rose_28 = ImageVector.Builder(
            name = "ic_rose_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.1787 2C17.4529 2.00004 19.3362 3.59493 19.834 5.7168C21.946 6.22716 23.5 8.14862 23.5 10.417C23.4999 12.1826 22.5571 13.7366 21.1436 14.5801V15.1943C21.1434 18.7384 18.6088 21.7048 15.25 22.3037V24.75C15.25 25.4404 14.6904 26 14 26C13.3099 25.9996 12.75 25.4401 12.75 24.75V22.3037C9.39144 21.7046 6.85754 18.7383 6.85742 15.1943V13.7129C5.41497 12.4949 4.5 10.6638 4.5 8.625C4.5 4.98226 7.41888 2 11.0537 2H15.1787ZM9.35742 15.1943C9.35755 17.8174 11.4522 19.9168 14 19.917C16.5299 19.917 18.6131 17.8472 18.6426 15.25H11.0537C10.4663 15.25 9.89812 15.1704 9.35742 15.0244V15.1943ZM11.0537 4.5C8.83101 4.5 7 6.33135 7 8.625C7 10.0549 7.71163 11.3051 8.78613 12.0439C8.79698 12.051 8.80775 12.0581 8.81836 12.0654C9.46104 12.4984 10.2304 12.75 11.0537 12.75H18.7139C19.9606 12.75 20.9998 11.7215 21 10.417C21 9.49231 20.4774 8.70756 19.7295 8.32812C19.1078 10.2585 17.3157 11.667 15.1787 11.667H11.7344C10.0527 11.6668 8.7168 10.2884 8.7168 8.625C8.7168 6.9616 10.0527 5.58325 11.7344 5.58301H14.127C14.8172 5.58301 15.3768 6.1428 15.377 6.83301C15.377 7.52336 14.8173 8.08301 14.127 8.08301H11.7344C11.4647 8.08325 11.2168 8.31071 11.2168 8.625C11.2168 8.93929 11.4647 9.16675 11.7344 9.16699H15.1787C16.4255 9.16694 17.4639 8.13763 17.4639 6.83301C17.4637 5.52854 16.4254 4.50005 15.1787 4.5H11.0537Z"),
            )
        }.build()
        return _ic_rose_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcRose28Preview() {
    Icon(
        imageVector = Icons.ic_rose_28,
        contentDescription = null,
    )
}