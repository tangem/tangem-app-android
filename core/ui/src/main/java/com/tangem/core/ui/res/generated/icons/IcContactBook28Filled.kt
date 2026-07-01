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

private var _ic_contact_book_28_filled: ImageVector? = null

val Icons.ic_contact_book_28_filled: ImageVector
    get() {
        if (_ic_contact_book_28_filled != null) return _ic_contact_book_28_filled!!
        _ic_contact_book_28_filled = ImageVector.Builder(
            name = "ic_contact_book_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.6289 2.99805C22.2158 2.99805 23.4786 4.30452 23.4795 5.88672L23.5 22.1113C23.4997 23.694 22.2375 25.0018 20.6504 25.002H7.37402C5.78796 25.0016 4.52505 23.6966 4.52344 22.1152L4.51953 20.9785H3.25C2.55964 20.9785 2 20.4189 2 19.7285C2.00026 19.0384 2.55981 18.4785 3.25 18.4785H4.50977L4.50293 16.585C4.5028 16.5381 4.50815 16.4909 4.5166 16.4453L4.51465 15.251H3.25C2.55981 15.251 2.00026 14.6911 2 14.001C2.00026 13.3108 2.55981 12.751 3.25 12.751H4.51172L4.50781 9.52344H3.25C2.55981 9.52344 2.00026 8.96357 2 8.27344C2.00026 7.58331 2.55981 7.02344 3.25 7.02344H4.50488L4.50293 5.88867C4.50326 4.30638 5.76586 2.99862 7.35254 2.99805H20.6289ZM12.3789 15.0342C11.0746 15.0379 10.0025 16.0287 9.87012 17.2988L9.85645 17.5566V17.8789C9.85645 18.2155 9.99054 18.5384 10.2285 18.7764C10.4664 19.0141 10.7887 19.1483 11.125 19.1484H16.875C17.5756 19.1482 18.1445 18.5796 18.1445 17.8789V17.5566C18.1407 16.2525 17.1498 15.1804 15.8799 15.0479L15.6221 15.0342H12.3789ZM14 8.85254C12.6466 8.85271 11.5488 9.95023 11.5488 11.3037C11.549 12.657 12.6467 13.7537 14 13.7539C15.3533 13.7537 16.45 12.657 16.4502 11.3037C16.4502 9.95023 15.3534 8.8527 14 8.85254Z"),
            )
        }.build()
        return _ic_contact_book_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcContactBook28FilledPreview() {
    Icon(
        imageVector = Icons.ic_contact_book_28_filled,
        contentDescription = null,
    )
}