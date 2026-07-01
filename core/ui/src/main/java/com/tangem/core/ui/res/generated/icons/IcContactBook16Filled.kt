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

private var _ic_contact_book_16_filled: ImageVector? = null

val Icons.ic_contact_book_16_filled: ImageVector
    get() {
        if (_ic_contact_book_16_filled != null) return _ic_contact_book_16_filled!!
        _ic_contact_book_16_filled = ImageVector.Builder(
            name = "ic_contact_book_16_filled",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.3643 2.01172C12.1353 2.08971 12.7425 2.73692 12.7432 3.53125L12.751 12.4688L12.7432 12.626C12.6641 13.3994 12.0063 13.997 11.2148 13.9971H4.79395C3.9502 13.997 3.25875 13.318 3.25781 12.4707L3.25586 11.8887H2.62793C2.28289 11.8887 2.00316 11.6087 2.00293 11.2637C2.00302 10.9186 2.28281 10.6387 2.62793 10.6387H3.25098L3.25 10.4951V8.62598H2.62793C2.28295 8.62598 2.00324 8.34589 2.00293 8.00098C2.00293 7.6558 2.28275 7.37598 2.62793 7.37598H3.25V5.36328H2.62793C2.28305 5.36328 2.00342 5.08304 2.00293 4.73828C2.00293 4.3931 2.28275 4.11328 2.62793 4.11328H3.25V3.53223L3.25879 3.375C3.33817 2.60188 3.99482 2.00398 4.78613 2.00391H11.207L11.3643 2.01172ZM7.29102 8.63574C6.59853 8.63775 6.03713 9.19911 6.03516 9.8916V10.0332C6.03525 10.2208 6.1105 10.4005 6.24316 10.5332C6.37584 10.6659 6.55554 10.7411 6.74316 10.7412H9.25684C9.64763 10.7411 9.96464 10.424 9.96484 10.0332V9.8916C9.96287 9.19909 9.4015 8.63772 8.70898 8.63574H7.29102ZM8 5.25879C7.32393 5.25895 6.77555 5.80733 6.77539 6.4834C6.77565 7.15938 7.32399 7.70785 8 7.70801C8.67606 7.70792 9.22435 7.15942 9.22461 6.4834C9.22445 5.80728 8.67613 5.25888 8 5.25879Z"),
            )
        }.build()
        return _ic_contact_book_16_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcContactBook16FilledPreview() {
    Icon(
        imageVector = Icons.ic_contact_book_16_filled,
        contentDescription = null,
    )
}