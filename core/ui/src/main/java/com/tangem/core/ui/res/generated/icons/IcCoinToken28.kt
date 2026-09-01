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

private var _ic_coin_token_28: ImageVector? = null

val Icons.ic_coin_token_28: ImageVector
    get() {
        if (_ic_coin_token_28 != null) return _ic_coin_token_28!!
        _ic_coin_token_28 = ImageVector.Builder(
            name = "ic_coin_token_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.88991 9.88965C10.3779 9.40184 11.1686 9.40178 11.6565 9.88965L14.8821 13.1162C15.3701 13.6045 15.3701 14.3965 14.8821 14.8848L11.6565 18.1113C11.1685 18.5995 10.378 18.5994 9.88991 18.1113L6.66433 14.8848C6.17629 14.3965 6.17632 13.6045 6.66433 13.1162L9.88991 9.88965ZM9.31472 14.001L10.7727 15.459L12.2317 14.001L10.7727 12.542L9.31472 14.001Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.2239 5.22168C22.0707 5.2219 25.9992 9.15246 25.9993 14C25.9993 18.8476 22.0707 22.7781 17.2239 22.7783C16.0852 22.7783 15.0025 22.5527 14.01 22.1592C10.8544 23.4102 7.12052 22.7609 4.56765 20.207C1.14119 16.7789 1.14119 11.2211 4.56765 7.79297C7.12041 5.23929 10.8545 4.58909 14.01 5.83984C15.0024 5.44638 16.0853 5.22169 17.2239 5.22168ZM13.3567 8.27832C11.0463 7.23464 8.23386 7.66115 6.33523 9.56055C3.88494 12.0123 3.885 15.9876 6.33523 18.4395C8.23335 20.3383 11.0446 20.7643 13.3548 19.7217C13.4208 19.6821 13.4908 19.6498 13.5628 19.623C14.157 19.3279 14.7151 18.9347 15.2102 18.4395C17.5842 16.0642 17.6584 12.2595 15.4329 9.79492L15.2102 9.56055L14.9759 9.33789C14.5429 8.94659 14.0683 8.62709 13.5676 8.37793C13.4946 8.35107 13.4238 8.31837 13.3567 8.27832ZM16.9124 7.73047C16.9339 7.75156 16.9565 7.77161 16.9778 7.79297C20.4046 11.2211 20.4046 16.7789 16.9778 20.207C16.9566 20.2282 16.9337 20.2476 16.9124 20.2686C17.0156 20.2738 17.1194 20.2783 17.2239 20.2783C20.6891 20.2781 23.4993 17.4678 23.4993 14C23.4992 10.5322 20.689 7.7219 17.2239 7.72168C17.1194 7.72168 17.0156 7.72525 16.9124 7.73047Z"),
            )
        }.build()
        return _ic_coin_token_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCoinToken28Preview() {
    Icon(
        imageVector = Icons.ic_coin_token_28,
        contentDescription = null,
    )
}