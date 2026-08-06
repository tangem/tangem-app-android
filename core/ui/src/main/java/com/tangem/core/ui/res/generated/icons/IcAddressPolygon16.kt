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

private var _ic_address_polygon_16: ImageVector? = null

val Icons.ic_address_polygon_16: ImageVector
    get() {
        if (_ic_address_polygon_16 != null) return _ic_address_polygon_16!!
        _ic_address_polygon_16 = ImageVector.Builder(
            name = "ic_address_polygon_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.53711 2.69414C5.02195 2.43596 5.60404 2.43592 6.08887 2.69414L7.75195 3.57988C8.28964 3.86657 8.62591 4.42656 8.62598 5.03594V5.49492C8.62547 5.83958 8.34573 6.11979 8.00098 6.11992C7.65613 6.1199 7.37649 5.83965 7.37598 5.49492V5.03594C7.37591 4.88841 7.29414 4.75294 7.16406 4.6834L5.50098 3.79766C5.38347 3.73507 5.24252 3.73512 5.125 3.79766L3.46191 4.6834C3.3317 4.7529 3.25007 4.88833 3.25 5.03594V7.38457C3.2502 7.53207 3.33176 7.66767 3.46191 7.73711L5.125 8.62285C5.24241 8.68521 5.3836 8.6853 5.50098 8.62285L9.91309 6.27324C10.3979 6.01506 10.98 6.01502 11.4648 6.27324L13.1279 7.15898C13.6656 7.44565 14.0019 8.00567 14.002 8.61504V10.9637C14.0018 11.573 13.6656 12.1331 13.1279 12.4197L11.4648 13.3055C10.9801 13.5636 10.3978 13.5635 9.91309 13.3055L8.25 12.4197C7.71226 12.1331 7.37617 11.573 7.37598 10.9637V10.5057C7.37598 10.1605 7.65581 9.88068 8.00098 9.88066C8.34604 9.8808 8.62598 10.1606 8.62598 10.5057V10.9637C8.62617 11.1113 8.70759 11.2478 8.83789 11.3172L10.501 12.2029C10.6183 12.2652 10.7597 12.2652 10.877 12.2029L12.54 11.3172C12.6703 11.2478 12.7518 11.1112 12.752 10.9637V8.61504C12.7519 8.46752 12.6701 8.33202 12.54 8.2625L10.877 7.37676C10.7595 7.31417 10.6185 7.31422 10.501 7.37676L6.08887 9.72637C5.60417 9.98446 5.02184 9.98437 4.53711 9.72637L2.87402 8.84062C2.33626 8.55404 2.0002 7.99392 2 7.38457V5.03594C2.00007 4.42648 2.3362 3.86654 2.87402 3.57988L4.53711 2.69414Z"),
            )
        }.build()
        return _ic_address_polygon_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAddressPolygon16Preview() {
    Icon(
        imageVector = Icons.ic_address_polygon_16,
        contentDescription = null,
    )
}