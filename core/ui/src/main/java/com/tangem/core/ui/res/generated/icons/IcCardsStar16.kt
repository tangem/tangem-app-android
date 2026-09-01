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

private var _ic_cards_star_16: ImageVector? = null

val Icons.ic_cards_star_16: ImageVector
    get() {
        if (_ic_cards_star_16 != null) return _ic_cards_star_16!!
        _ic_cards_star_16 = ImageVector.Builder(
            name = "ic_cards_star_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.7283 5.43164C10.0733 5.43184 10.3533 5.71158 10.3533 6.05664C10.3538 6.24757 10.5089 6.40234 10.7 6.40234C11.0448 6.40262 11.3248 6.68249 11.325 7.02734C11.3247 7.37212 11.0448 7.65206 10.7 7.65234C10.5324 7.65234 10.3923 7.77162 10.3601 7.92969L10.3533 7.99902L10.3406 8.125C10.2823 8.40957 10.0301 8.62385 9.7283 8.62402C9.38329 8.62402 9.10357 8.34398 9.1033 7.99902C9.10325 7.90735 9.06646 7.81882 9.00174 7.75391C8.93681 7.68903 8.84841 7.6524 8.75662 7.65234C8.41161 7.65234 8.13189 7.3723 8.13162 7.02734C8.1318 6.68232 8.41156 6.40234 8.75662 6.40234C8.94758 6.40223 9.10278 6.2475 9.1033 6.05664C9.1033 5.71146 9.38312 5.43164 9.7283 5.43164Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.3377 3C12.2551 3.00012 12.9996 3.74473 12.9998 4.66211V9.39355C12.9993 10.3106 12.2548 11.0546 11.3377 11.0547H10.7195C10.6895 11.1496 10.65 11.2426 10.5994 11.3301C10.4159 11.6467 10.1132 11.8777 9.75955 11.9717L9.75857 11.9707L6.11209 12.9521L6.11111 12.9531C5.37483 13.1498 4.6183 12.713 4.41873 11.9775L4.41775 11.9766L3.04764 6.86426L3.04666 6.86133C2.85276 6.12631 3.28716 5.37193 4.02029 5.1709L4.0242 5.16992L6.46463 4.51855C6.53756 3.66845 7.24996 3.00015 8.11893 3H11.3377ZM4.35037 6.37598C4.27866 6.39583 4.23663 6.47098 4.25565 6.54297L5.62576 11.6523C5.64569 11.7222 5.71744 11.7637 5.78787 11.7451L8.35525 11.0547H8.11893C7.2018 11.0545 6.45831 10.3106 6.45779 9.39355V5.81445L4.35037 6.37598ZM8.11893 4.25098C7.89193 4.25113 7.70795 4.43512 7.70779 4.66211V9.39355C7.70831 9.62024 7.89216 9.80453 8.11893 9.80469H11.3377C11.5645 9.80457 11.7493 9.62026 11.7498 9.39355V4.66211C11.7496 4.43509 11.5647 4.25109 11.3377 4.25098H8.11893Z"),
            )
        }.build()
        return _ic_cards_star_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCardsStar16Preview() {
    Icon(
        imageVector = Icons.ic_cards_star_16,
        contentDescription = null,
    )
}