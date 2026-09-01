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

private var _ic_basketball_24: ImageVector? = null

val Icons.ic_basketball_24: ImageVector
    get() {
        if (_ic_basketball_24 != null) return _ic_basketball_24!!
        _ic_basketball_24 = ImageVector.Builder(
            name = "ic_basketball_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.93081 4.92898C8.83556 1.02423 15.1667 1.02426 19.0714 4.92898L19.4259 5.30203C21.127 7.18481 21.9824 9.55856 21.9972 11.9368C21.9985 11.9572 22.0001 11.9776 22.0001 11.9983C22.0001 12.0188 21.9984 12.0396 21.9972 12.0598C21.9819 14.5985 21.0083 17.1327 19.0714 19.0696C15.1667 22.974 8.83548 22.974 4.93081 19.0696C1.02619 15.1649 1.02636 8.83374 4.93081 4.92898ZM7.0978 18.3157C8.27461 19.2315 9.64714 19.7721 11.0568 19.9387C11.2661 17.9506 12.0574 16.1362 13.258 14.6692L12.0011 13.4124L7.0978 18.3157ZM14.6828 16.093C13.8372 17.1806 13.2666 18.4914 13.0744 19.9231C14.4389 19.7393 15.7635 19.2028 16.9044 18.3147L14.6828 16.093ZM9.33023 10.7405C7.86282 11.9415 6.04862 12.7336 4.05972 12.9427C4.22635 14.3521 4.76727 15.7249 5.68276 16.9016L10.5871 11.9973L9.33023 10.7405ZM19.9259 13.0725C18.4948 13.2646 17.1842 13.8341 16.0968 14.679L18.3185 16.9007C19.2058 15.7602 19.7421 14.4363 19.9259 13.0725ZM13.4162 11.9983L14.673 13.2552C16.1399 12.0551 17.9538 11.2631 19.9416 11.054C19.7748 9.64443 19.2343 8.27173 18.3185 7.095L13.4162 11.9983ZM5.68374 7.09402C4.79578 8.23478 4.25925 9.55979 4.07534 10.9241C5.50757 10.7321 6.8185 10.1625 7.9064 9.31668L5.68374 7.09402ZM12.9455 4.05691C12.7366 6.04531 11.9447 7.85919 10.7443 9.32644L12.0011 10.5833L16.9035 5.68093C15.7268 4.76554 14.3548 4.22358 12.9455 4.05691ZM10.9269 4.07254C9.56305 4.25632 8.23834 4.7926 7.0978 5.67996L9.32046 7.90261C10.1657 6.81491 10.735 5.50427 10.9269 4.07254Z"),
            )
        }.build()
        return _ic_basketball_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBasketball24Preview() {
    Icon(
        imageVector = Icons.ic_basketball_24,
        contentDescription = null,
    )
}