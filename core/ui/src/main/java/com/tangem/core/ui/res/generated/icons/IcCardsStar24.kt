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

private var _ic_cards_star_24: ImageVector? = null

val Icons.ic_cards_star_24: ImageVector
    get() {
        if (_ic_cards_star_24 != null) return _ic_cards_star_24!!
        _ic_cards_star_24 = ImageVector.Builder(
            name = "ic_cards_star_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.1614 7.44434C15.7135 7.44441 16.1612 7.89221 16.1614 8.44434C16.1614 8.87314 16.509 9.22044 16.9377 9.2207C17.4898 9.2207 17.9374 9.66869 17.9377 10.2207C17.9377 10.773 17.49 11.2207 16.9377 11.2207C16.536 11.221 16.2053 11.5267 16.1653 11.918L16.1565 12.0996C16.1055 12.604 15.6792 12.998 15.1614 12.998C14.6093 12.9979 14.1615 12.5501 14.1614 11.998C14.1613 11.7921 14.0794 11.5938 13.9338 11.4482C13.7883 11.3029 13.5906 11.2208 13.385 11.2207C12.8328 11.2205 12.385 10.7729 12.385 10.2207C12.3853 9.70324 12.7784 9.2778 13.2824 9.22656L13.385 9.2207L13.4641 9.2168C13.8557 9.17693 14.1614 8.84643 14.1614 8.44434C14.1615 7.89226 14.6093 7.44449 15.1614 7.44434Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.2717 3C19.7779 3 20.9991 4.22137 20.9992 5.72754V14.7148C20.9992 16.2211 19.778 17.4424 18.2717 17.4424H16.8655C16.8119 17.6415 16.7342 17.835 16.6291 18.0166C16.3131 18.5624 15.7931 18.9595 15.1838 19.1221L8.50999 20.918L8.50901 20.9189C7.23832 21.2584 5.93212 20.5058 5.58811 19.2363L5.58714 19.2334L3.08128 9.88477L3.0803 9.88086C2.74541 8.61237 3.49473 7.3108 4.75999 6.96387L4.76682 6.96191L9.32249 5.74609V5.72754C9.32262 4.22157 10.5431 3.00032 12.0491 3H18.2717ZM5.2844 8.89355C5.08121 8.95176 4.96107 9.16126 5.01292 9.36621L7.5178 18.7129C7.57347 18.9184 7.78477 19.0403 7.99046 18.9863L13.7317 17.4424H12.0491C10.5431 17.4421 9.3225 16.2209 9.32249 14.7148V7.81641L5.2844 8.89355ZM12.0491 5C11.6477 5.00032 11.3226 5.32614 11.3225 5.72754V14.7148C11.3225 15.1163 11.6476 15.4421 12.0491 15.4424H18.2717C18.6734 15.4424 18.9992 15.1165 18.9992 14.7148V5.72754C18.9991 5.32594 18.6733 5 18.2717 5H12.0491Z"),
            )
        }.build()
        return _ic_cards_star_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCardsStar24Preview() {
    Icon(
        imageVector = Icons.ic_cards_star_24,
        contentDescription = null,
    )
}