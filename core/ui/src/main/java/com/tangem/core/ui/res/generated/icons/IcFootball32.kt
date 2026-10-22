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

private var _ic_football_32: ImageVector? = null

val Icons.ic_football_32: ImageVector
    get() {
        if (_ic_football_32 != null) return _ic_football_32!!
        _ic_football_32 = ImageVector.Builder(
            name = "ic_football_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.45404 6.45407C11.7261 1.18199 20.2738 1.18196 25.5458 6.45407L25.789 6.70309C30.8167 11.992 30.7356 20.3561 25.5458 25.5459C20.2738 30.818 11.7261 30.8179 6.45404 25.5459C1.182 20.2738 1.18197 11.7261 6.45404 6.45407ZM13.7294 21.6875L13.0781 22.5849L14.4423 26.3828C15.4744 26.5368 16.5245 26.5367 17.5566 26.3828L18.9208 22.5849L18.2695 21.6875H13.7294ZM8.93939 23.7705C9.5195 24.2984 10.1449 24.7483 10.8017 25.123L10.332 23.8134L8.93939 23.7705ZM21.6669 23.8134L21.1962 25.123C21.8533 24.7482 22.4791 24.2986 23.0595 23.7705L21.6669 23.8134ZM5.6474 17.7422C5.81786 18.7604 6.13879 19.7574 6.61029 20.6992L10.6513 20.8222L11.3027 19.9228L9.90131 15.6123L8.84369 15.2685L5.6474 17.7422ZM22.0956 15.6123L20.6953 19.9228L21.3476 20.8222L25.3886 20.6992C25.8598 19.7581 26.1829 18.7625 26.3535 17.7451L23.1523 15.2685L22.0956 15.6123ZM12.8544 14.9922L14.0556 18.6875H17.9423L19.1425 14.9922L15.999 12.708L12.8544 14.9922ZM25.166 13.0332L26.2871 13.9013C26.1319 13.1373 25.8908 12.3878 25.5644 11.667L25.166 13.0332ZM6.43158 11.6709C6.10678 12.3891 5.86765 13.1363 5.71283 13.8974L6.83002 13.0332L6.43158 11.6709ZM11.1367 6.69137C10.239 7.16068 9.39538 7.76889 8.63666 8.51559L9.77533 12.416L10.8261 12.7578L14.499 10.0888V8.97946L11.1367 6.69137ZM17.499 8.98141V10.0888L21.1718 12.7578L22.2216 12.417L23.3603 8.51364L23.0331 8.20309C22.3623 7.59691 21.6309 7.09463 20.8613 6.69235L17.499 8.98141ZM17.1835 5.56637C16.3972 5.47767 15.6027 5.4777 14.8163 5.56637L15.9999 6.37301L17.1835 5.56637Z"),
            )
        }.build()
        return _ic_football_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcFootball32Preview() {
    Icon(
        imageVector = Icons.ic_football_32,
        contentDescription = null,
    )
}