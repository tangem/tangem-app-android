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

private var _ic_filter_24: ImageVector? = null

val Icons.ic_filter_24: ImageVector
    get() {
        if (_ic_filter_24 != null) return _ic_filter_24!!
        _ic_filter_24 = ImageVector.Builder(
            name = "ic_filter_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.38867 12.7432C11.3194 12.7432 12.933 14.0774 13.3779 15.8711H20C20.5521 15.8711 20.9998 16.319 21 16.8711C21 17.4234 20.5523 17.8711 20 17.8711H13.3779C12.9333 19.6652 11.3197 21 9.38867 21C7.45775 20.9999 5.84503 19.6651 5.40039 17.8711H4C3.44772 17.8711 3 17.4234 3 16.8711C3.00025 16.319 3.44787 15.8711 4 15.8711H5.40039C5.84533 14.0775 7.45806 12.7433 9.38867 12.7432ZM9.38867 14.7432C8.22823 14.7433 7.27759 15.6906 7.27734 16.8711C7.27734 18.0518 8.22808 18.9999 9.38867 19C10.5494 19 11.5 18.0519 11.5 16.8711C11.4998 15.6905 10.5492 14.7432 9.38867 14.7432ZM14.3887 3C16.3197 3 17.9333 4.33478 18.3779 6.12891H20C20.5523 6.12892 21 6.57662 21 7.12891C20.9998 7.68102 20.5522 8.12891 20 8.12891H18.3779C17.933 9.92263 16.3194 11.2568 14.3887 11.2568C12.4581 11.2567 10.8453 9.9225 10.4004 8.12891H4C3.44784 8.12891 3.0002 7.68102 3 7.12891C3 6.57662 3.44772 6.12891 4 6.12891H10.4004C10.845 4.33492 12.4578 3.0001 14.3887 3ZM14.3887 5C13.2281 5.00012 12.2773 5.94819 12.2773 7.12891C12.2776 8.30943 13.2282 9.25672 14.3887 9.25684C15.5492 9.25684 16.4998 8.3095 16.5 7.12891C16.5 5.94812 15.5494 5 14.3887 5Z"),
            )
        }.build()
        return _ic_filter_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcFilter24Preview() {
    Icon(
        imageVector = Icons.ic_filter_24,
        contentDescription = null,
    )
}