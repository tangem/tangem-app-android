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

private var _ic_arrow_cross_horizontal_16: ImageVector? = null

val Icons.ic_arrow_cross_horizontal_16: ImageVector
    get() {
        if (_ic_arrow_cross_horizontal_16 != null) return _ic_arrow_cross_horizontal_16!!
        _ic_arrow_cross_horizontal_16 = ImageVector.Builder(
            name = "ic_arrow_cross_horizontal_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.81738 8.70704C8.09271 8.49885 8.48517 8.55281 8.69336 8.82814L9.13086 9.40724C9.31691 9.65308 9.57741 9.78614 9.84766 9.78614H10.8789C10.7221 9.54477 10.748 9.21857 10.959 9.00587C11.2021 8.761 11.5978 8.75993 11.8428 9.00294L12.8154 9.96681C12.9337 10.0842 13 10.2445 13 10.4111C12.9999 10.5776 12.9336 10.7373 12.8154 10.8545L11.8428 11.8184C11.5977 12.0614 11.2021 12.0604 10.959 11.8154C10.7483 11.603 10.7218 11.2774 10.8779 11.0361H9.84766C9.14595 11.0361 8.53106 10.6863 8.13379 10.1611L7.69629 9.58204C7.48815 9.30677 7.54219 8.91526 7.81738 8.70704ZM10.959 4.18458C11.2021 3.93964 11.5977 3.93862 11.8428 4.18165L12.8154 5.14552C12.9336 5.26276 12.9999 5.42245 13 5.58888C13 5.75549 12.9337 5.91587 12.8154 6.03321L11.8428 6.99708C11.5978 7.24009 11.2021 7.23903 10.959 6.99415C10.748 6.78145 10.7221 6.45525 10.8789 6.21388H9.84766C9.57741 6.21388 9.31691 6.34695 9.13086 6.59279L9.12891 6.59474L6.40723 10.1602L6.4082 10.1611C6.01093 10.6864 5.39611 11.0361 4.69434 11.0361H3.625C3.27996 11.0361 3.00022 10.7561 3 10.4111C3 10.066 3.27982 9.78614 3.625 9.78614H4.69434C4.96464 9.78614 5.22508 9.65318 5.41113 9.40724L5.41309 9.40529L8.13477 5.83693C8.53208 5.31269 9.14672 4.96388 9.84766 4.96388H10.8779C10.7218 4.72261 10.7483 4.39698 10.959 4.18458ZM4.69434 4.96388C5.39611 4.96388 6.01093 5.31361 6.4082 5.83888L6.8457 6.41798C7.05366 6.69318 6.99956 7.08476 6.72461 7.29298C6.44933 7.50114 6.05685 7.4471 5.84863 7.17189L5.41113 6.59279C5.22508 6.34685 4.96463 6.21388 4.69434 6.21388H3.625C3.27982 6.21388 3 5.93406 3 5.58888C3.00023 5.24389 3.27996 4.96388 3.625 4.96388H4.69434Z"),
            )
        }.build()
        return _ic_arrow_cross_horizontal_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowCrossHorizontal16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_cross_horizontal_16,
        contentDescription = null,
    )
}