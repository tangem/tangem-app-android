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

private var _ic_graduation_cap_32: ImageVector? = null

val Icons.ic_graduation_cap_32: ImageVector
    get() {
        if (_ic_graduation_cap_32 != null) return _ic_graduation_cap_32!!
        _ic_graduation_cap_32 = ImageVector.Builder(
            name = "ic_graduation_cap_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.4668 4.92753C14.5949 3.72945 17.1692 3.69204 19.3252 4.81522L19.5332 4.92753L27.0508 9.16483C29.649 10.6293 29.6491 14.4006 27.0508 15.865L24.8154 17.1248V21.0945C24.8153 22.7571 23.9951 24.3196 22.6142 25.2508L20.6582 26.5721C17.8374 28.4758 14.1614 28.4754 11.3398 26.5721L9.38279 25.2517C8.00452 24.3213 7.18268 22.7597 7.1826 21.0945V17.1238L4.9492 15.865C2.35059 14.4006 2.35059 10.6292 4.9492 9.16483L12.4668 4.92753ZM10.1826 21.0945C10.1827 21.7737 10.5176 22.3988 11.0605 22.7654L13.0176 24.0857C14.8254 25.3051 17.1743 25.3048 18.9804 24.0857L20.9375 22.7644C21.48 22.3986 21.8153 21.7736 21.8154 21.0945V18.8152L19.5332 20.1023C17.4052 21.3004 14.8292 21.3382 12.6738 20.2146L12.4668 20.1023L10.1826 18.8142V21.0945ZM18.0615 7.54178C16.7784 6.81941 15.2216 6.81946 13.9385 7.54178L13.9375 7.54081L6.42185 11.7781C5.85947 12.095 5.85947 12.9348 6.42185 13.2517L9.38474 14.9217C9.39685 14.9281 9.40897 14.9345 9.42088 14.9412L13.9394 17.4881L14.1826 17.615C15.4142 18.2072 16.8575 18.1652 18.0605 17.4881L22.5498 14.9568C22.5744 14.9422 22.5995 14.9281 22.625 14.9148L25.5781 13.2517L25.6767 13.1863C26.1076 12.8555 26.1074 12.1744 25.6767 11.8435L25.5781 11.7781L18.0615 7.54178Z"),
            )
        }.build()
        return _ic_graduation_cap_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGraduationCap32Preview() {
    Icon(
        imageVector = Icons.ic_graduation_cap_32,
        contentDescription = null,
    )
}