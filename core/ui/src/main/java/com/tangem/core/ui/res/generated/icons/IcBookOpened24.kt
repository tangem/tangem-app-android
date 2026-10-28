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

private var _ic_book_opened_24: ImageVector? = null

val Icons.ic_book_opened_24: ImageVector
    get() {
        if (_ic_book_opened_24 != null) return _ic_book_opened_24!!
        _ic_book_opened_24 = ImageVector.Builder(
            name = "ic_book_opened_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99512 2.99512C9.32109 2.99512 10.5936 3.52138 11.5312 4.45898C11.7002 4.62794 11.8555 4.80801 11.9971 4.99707C12.9092 3.78163 14.3626 2.99512 15.999 2.99512H19.5C20.1627 2.99528 20.798 3.25895 21.2666 3.72754C21.7352 4.19613 21.9989 4.83149 21.999 5.49414V17.4951C21.999 18.873 20.8837 19.9915 19.5059 19.9951H15.2373C14.2451 19.9938 13.3373 20.5539 12.8936 21.4414C12.6863 21.8562 12.2209 22.0741 11.7695 21.9678C11.4724 21.8977 11.2347 21.6982 11.1055 21.4385L11.1006 21.4414C10.6569 20.5538 9.74916 19.9938 8.75684 19.9951H4.49414C3.83144 19.995 3.19617 19.7313 2.72754 19.2627C2.25881 18.794 1.99512 18.158 1.99512 17.4951V5.49414C1.99522 4.8314 2.25889 4.19618 2.72754 3.72754C3.19618 3.25889 3.8314 2.99522 4.49414 2.99512H7.99512ZM4.49414 4.99512C4.36182 4.99522 4.23516 5.04804 4.1416 5.1416C4.04804 5.23516 3.99522 5.36182 3.99512 5.49414V17.4951C3.99512 17.6276 4.04795 17.755 4.1416 17.8486C4.23516 17.9421 4.36187 17.995 4.49414 17.9951H8.75488C9.55436 17.9942 10.3217 18.2014 10.9951 18.5742V7.99512C10.9951 7.19957 10.6787 6.43656 10.1162 5.87402C9.55368 5.31149 8.79066 4.99512 7.99512 4.99512H4.49414ZM15.999 4.99512C14.3425 4.99512 12.9992 6.3386 12.999 7.99512V18.5742C13.6724 18.2015 14.4399 17.9942 15.2393 17.9951H19.501C19.7761 17.9942 19.999 17.7703 19.999 17.4951V5.49414C19.9989 5.36191 19.946 5.23511 19.8525 5.1416C19.759 5.0481 19.6322 4.99528 19.5 4.99512H15.999Z"),
            )
        }.build()
        return _ic_book_opened_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBookOpened24Preview() {
    Icon(
        imageVector = Icons.ic_book_opened_24,
        contentDescription = null,
    )
}