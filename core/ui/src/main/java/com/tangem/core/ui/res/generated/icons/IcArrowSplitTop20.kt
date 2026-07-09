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

private var _ic_arrow_split_top_20: ImageVector? = null

val Icons.ic_arrow_split_top_20: ImageVector
    get() {
        if (_ic_arrow_split_top_20 != null) return _ic_arrow_split_top_20!!
        _ic_arrow_split_top_20 = ImageVector.Builder(
            name = "ic_arrow_split_top_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.21632 7.53715C3.79528 8.07662 3.23917 7.87829 3.04056 7.18808L2.03958 3.75294C1.88069 3.20553 2.21435 2.77713 2.78635 2.80093L6.36924 2.91993C7.08423 2.94373 7.402 3.44354 6.98889 3.97507L6.27391 4.88741L6.30568 4.90327C7.76744 5.87115 9.55491 7.88622 9.98391 9.21902H10.0157C10.4367 7.89415 12.2321 5.87115 13.6939 4.90327L13.7336 4.87947L13.0027 3.87194C12.6055 3.32454 12.9312 2.83267 13.6542 2.8406L17.2371 2.82473C17.8091 2.82473 18.1268 3.269 17.9521 3.8164L16.8399 7.21188C16.6254 7.89415 16.0534 8.07662 15.6482 7.51335L14.8776 6.45028L14.7028 6.56135C13.106 7.57682 10.9611 10.0758 10.9611 11.8926V16.1369C10.9611 16.8271 10.6195 17.2 9.99979 17.2C9.38014 17.2 9.04647 16.8271 9.04647 16.1369V11.8926C9.04647 10.0758 6.88562 7.57682 5.29675 6.56135L5.08226 6.42648L4.21632 7.53715Z"),
            )
        }.build()
        return _ic_arrow_split_top_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowSplitTop20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_split_top_20,
        contentDescription = null,
    )
}