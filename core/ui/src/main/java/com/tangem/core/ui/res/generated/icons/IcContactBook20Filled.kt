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

private var _ic_contact_book_20_filled: ImageVector? = null

val Icons.ic_contact_book_20_filled: ImageVector
    get() {
        if (_ic_contact_book_20_filled != null) return _ic_contact_book_20_filled!!
        _ic_contact_book_20_filled = ImageVector.Builder(
            name = "ic_contact_book_20_filled",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.8491 3.00195C15.031 3.00195 15.9994 3.95451 15.9995 5.1416V14.8623C15.9992 16.0493 15.0309 17.001 13.8491 17.001H6.1499C4.96831 17.0007 3.99978 16.0491 3.99951 14.8623V14.3857H3.24951C2.83552 14.3855 2.49951 14.0498 2.49951 13.6357C2.49989 13.222 2.83576 12.886 3.24951 12.8857H3.99951V10.749H3.24951C2.83552 10.7488 2.49951 10.4131 2.49951 9.99902C2.49997 9.58536 2.8358 9.24929 3.24951 9.24902H3.99951V7.11328H3.24951C2.83552 7.11302 2.49951 6.77733 2.49951 6.36328C2.49978 5.94946 2.83569 5.61354 3.24951 5.61328H3.99951V5.1416C3.99962 3.95467 4.96822 3.00221 6.1499 3.00195H13.8491ZM9.00732 10.5605C8.09866 10.5634 7.35222 11.2537 7.26025 12.1387L7.25049 12.3174V12.5156C7.2506 12.7783 7.35477 13.031 7.54053 13.2168C7.72634 13.4023 7.97909 13.5068 8.2417 13.5068H11.7593C12.3063 13.5066 12.7503 13.0626 12.7505 12.5156V12.3174C12.7477 11.4089 12.0571 10.6625 11.1724 10.5703L10.9937 10.5605H9.00732ZM9.99951 6.49316C9.0533 6.49344 8.28605 7.26085 8.28564 8.20703C8.28564 9.15356 9.05305 9.9216 9.99951 9.92188C10.9462 9.92188 11.7144 9.15372 11.7144 8.20703C11.7139 7.26069 10.946 6.49316 9.99951 6.49316Z"),
            )
        }.build()
        return _ic_contact_book_20_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcContactBook20FilledPreview() {
    Icon(
        imageVector = Icons.ic_contact_book_20_filled,
        contentDescription = null,
    )
}