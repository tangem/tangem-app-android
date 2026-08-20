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

private var _ic_banknote_wave_24: ImageVector? = null

val Icons.ic_banknote_wave_24: ImageVector
    get() {
        if (_ic_banknote_wave_24 != null) return _ic_banknote_wave_24!!
        _ic_banknote_wave_24 = ImageVector.Builder(
            name = "ic_banknote_wave_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.00098 13.9984C7.55311 13.9986 8.00098 14.4461 8.00098 14.9981C8.00079 15.5499 7.55299 15.9976 7.00098 15.9978H6.00098C5.44881 15.9978 5.00117 15.55 5.00098 14.9981C5.00098 14.446 5.44869 13.9984 6.00098 13.9984H7.00098Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M12 8.49742C13.9331 8.49761 15.4989 10.065 15.499 11.9972C15.499 13.9294 13.9332 15.4968 12 15.4969C10.0668 15.4968 8.5 13.9295 8.5 11.9972C8.50009 10.065 10.0669 8.49755 12 8.49742ZM12 10.4967C11.1725 10.4969 10.5001 11.1681 10.5 11.9972C10.5 12.8263 11.1724 13.4975 12 13.4976C12.8275 13.4974 13.499 12.8263 13.499 11.9972C13.4989 11.1682 12.8275 10.4969 12 10.4967Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.999 7.99759C18.5513 7.99763 18.999 8.44518 18.999 8.99724C18.999 9.54926 18.5512 9.99685 17.999 9.9969H16.999C16.4469 9.99677 15.9991 9.54921 15.999 8.99724C15.999 8.44523 16.4469 7.99772 16.999 7.99759H17.999Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M11.9932 4.54175C14.6144 3.88632 17.3496 3.82141 19.999 4.35139L20.3906 4.42949C21.3256 4.61653 21.9979 5.438 21.998 6.39072V17.2297C21.9981 17.8286 21.7298 18.3962 21.2666 18.7761C20.8033 19.1559 20.1941 19.3085 19.6064 19.191C17.2475 18.7191 14.8124 18.7773 12.4785 19.3608L12.2422 19.4194C9.46518 20.1138 6.56861 20.183 3.76172 19.6215L3.6084 19.5912C2.67348 19.404 2.00097 18.5827 2.00098 17.63V6.79097C2.00101 6.19227 2.26935 5.62451 2.73242 5.24463C3.1957 4.86476 3.80593 4.71227 4.39355 4.82974C6.7524 5.30155 9.18676 5.2434 11.5205 4.65988L11.9932 4.54175ZM19.6064 6.31165C17.2475 5.83979 14.8123 5.89795 12.4785 6.48151L12.0059 6.59963C9.54828 7.21412 6.99125 7.30998 4.49805 6.88274L4.00098 6.79V17.631L4.1543 17.6612L4.62695 17.7491C6.99466 18.1547 9.42296 18.0642 11.7568 17.4806L11.9932 17.4211C14.6142 16.7657 17.3488 16.7 19.998 17.2297V6.39072L19.6064 6.31165Z"),
            )
        }.build()
        return _ic_banknote_wave_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBanknoteWave24Preview() {
    Icon(
        imageVector = Icons.ic_banknote_wave_24,
        contentDescription = null,
    )
}