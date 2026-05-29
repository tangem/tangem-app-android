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

private var _ic_sign_usd_16: ImageVector? = null

val Icons.ic_sign_usd_16: ImageVector
    get() {
        if (_ic_sign_usd_16 != null) return _ic_sign_usd_16!!
        _ic_sign_usd_16 = ImageVector.Builder(
            name = "ic_sign_usd_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.26727 1.99707C8.61219 1.99733 8.8922 2.27711 8.89227 2.62207V3.1416C9.60171 3.25053 10.2502 3.48588 10.7751 3.8252C11.5077 4.2989 12.0699 5.02749 12.07 5.92578C12.0698 6.27076 11.79 6.55078 11.445 6.55078C11.1002 6.5505 10.8202 6.27058 10.82 5.92578C10.8199 5.59353 10.611 5.20778 10.0964 4.875C9.58774 4.54621 8.84725 4.32234 7.99774 4.32227C7.14836 4.32233 6.40775 4.5463 5.89911 4.875C5.3846 5.20775 5.17464 5.59357 5.1745 5.92578C5.17455 6.1534 5.21746 6.32111 5.28583 6.44824C5.35271 6.57247 5.46287 6.6936 5.65106 6.80469C6.0523 7.04141 6.77633 7.21188 7.99774 7.21191C9.27898 7.21195 10.3557 7.36993 11.1286 7.81152C11.5276 8.03958 11.8516 8.34625 12.071 8.74316C12.2889 9.13756 12.3835 9.58496 12.3835 10.0654C12.3833 11.1381 11.7909 11.8864 10.9479 12.3301C10.3645 12.637 9.65185 12.8058 8.89227 12.877V13.3682C8.89227 13.7132 8.61223 13.9929 8.26727 13.9932C7.92209 13.9932 7.64227 13.7133 7.64227 13.3682V12.9072C6.64705 12.8568 5.72816 12.5988 5.01923 12.1787C4.24036 11.7171 3.61122 10.9903 3.61102 10.0654C3.61102 9.72037 3.89101 9.44062 4.23602 9.44043C4.5812 9.44043 4.86102 9.72025 4.86102 10.0654C4.86123 10.3711 5.07429 10.7587 5.65594 11.1035C6.22446 11.4404 7.05168 11.6689 7.99774 11.6689C8.98847 11.6689 9.81303 11.5144 10.3659 11.2236C10.8839 10.951 11.1333 10.5843 11.1335 10.0654C11.1335 9.75012 11.0716 9.52032 10.9763 9.34766C10.8822 9.17762 10.736 9.02654 10.5085 8.89648C10.0277 8.6218 9.2235 8.46195 7.99774 8.46191C6.71223 8.46188 5.71107 8.2915 5.01532 7.88086C4.65545 7.6684 4.373 7.38905 4.18524 7.04004C3.99905 6.69377 3.92456 6.31327 3.9245 5.92578C3.92464 5.02755 4.48785 4.2989 5.2204 3.8252C5.87568 3.40167 6.7237 3.13754 7.64227 3.08301V2.62207C7.64234 2.27695 7.92214 1.99707 8.26727 1.99707Z"),
            )
        }.build()
        return _ic_sign_usd_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignUsd16Preview() {
    Icon(
        imageVector = Icons.ic_sign_usd_16,
        contentDescription = null,
    )
}