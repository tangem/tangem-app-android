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

private var _ic_chair_16: ImageVector? = null

val Icons.ic_chair_16: ImageVector
    get() {
        if (_ic_chair_16 != null) return _ic_chair_16!!
        _ic_chair_16 = ImageVector.Builder(
            name = "ic_chair_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.9442 3C11.0946 3.00013 12.028 3.93259 12.0282 5.08301V7.00879C12.2765 7.11117 12.4984 7.27612 12.6678 7.49316C12.9646 7.87362 13.0699 8.36976 12.953 8.83789L12.4579 10.8184C12.3846 11.111 12.2321 11.3667 12.0282 11.5674V12.375C12.0282 12.7201 11.7482 12.9998 11.4032 13C11.058 13 10.7782 12.7202 10.7782 12.375V12.0283H5.22154V12.375C5.22153 12.7201 4.94167 13 4.59654 13C4.25148 12.9999 3.97155 12.7201 3.97154 12.375V11.5674C3.7676 11.3666 3.61512 11.111 3.54185 10.8184L3.04674 8.83789C2.92977 8.36971 3.03509 7.87364 3.33189 7.49316C3.50141 7.27607 3.72312 7.11117 3.97154 7.00879V5.08301C3.97172 3.93266 4.90426 3.00026 6.05455 3H9.9442ZM11.4315 8.13965C11.2854 8.13966 11.1583 8.23925 11.1229 8.38086L10.8153 9.61035C10.7456 9.88831 10.4954 10.0839 10.2088 10.084H5.79088C5.50426 10.084 5.25416 9.88832 5.18443 9.61035L4.87681 8.38086C4.84142 8.2393 4.71425 8.13972 4.56822 8.13965C4.47027 8.13965 4.37756 8.1846 4.31724 8.26172C4.25709 8.33892 4.23589 8.44022 4.25963 8.53516L4.75474 10.5156C4.7936 10.6698 4.93246 10.7783 5.09166 10.7783H10.9081C11.0672 10.7783 11.2061 10.6698 11.245 10.5156L11.7401 8.53516C11.7638 8.44027 11.7426 8.3389 11.6825 8.26172C11.6222 8.18462 11.5294 8.13968 11.4315 8.13965ZM6.05455 4.25C5.5945 4.25026 5.22172 4.62314 5.22154 5.08301V7.03223C5.6441 7.22612 5.97123 7.60368 6.08971 8.07715L6.27916 8.83398H9.72056L9.91002 8.07715C10.0285 7.60361 10.3555 7.2261 10.7782 7.03223V5.08301C10.778 4.62306 10.4044 4.25013 9.9442 4.25H6.05455Z"),
            )
        }.build()
        return _ic_chair_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChair16Preview() {
    Icon(
        imageVector = Icons.ic_chair_16,
        contentDescription = null,
    )
}