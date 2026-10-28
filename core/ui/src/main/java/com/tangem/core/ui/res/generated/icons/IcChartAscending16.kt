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

private var _ic_chart_ascending_16: ImageVector? = null

val Icons.ic_chart_ascending_16: ImageVector
    get() {
        if (_ic_chart_ascending_16 != null) return _ic_chart_ascending_16!!
        _ic_chart_ascending_16 = ImageVector.Builder(
            name = "ic_chart_ascending_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.625 3.5C3.97018 3.5 4.25 3.77982 4.25 4.125V11.25H12.374C12.7192 11.25 12.999 11.5298 12.999 11.875C12.999 12.2202 12.7192 12.5 12.374 12.5H3.625C3.27982 12.5 3 12.2202 3 11.875V4.125C3 3.77982 3.27982 3.5 3.625 3.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.4863 5.16504C12.4915 5.16597 12.4968 5.16593 12.502 5.16699C12.532 5.17322 12.5609 5.18368 12.5898 5.19434C12.5994 5.19786 12.6098 5.19916 12.6191 5.20312C12.6385 5.21135 12.6562 5.22314 12.6748 5.2334C12.6922 5.243 12.7102 5.25152 12.7266 5.2627C12.7366 5.26958 12.7451 5.27859 12.7549 5.28613C12.7786 5.30441 12.8024 5.32233 12.8232 5.34375L12.8262 5.34668C12.8309 5.35159 12.8344 5.35826 12.8389 5.36328C12.8609 5.38786 12.8814 5.41356 12.8994 5.44141C12.9067 5.45276 12.9124 5.46487 12.9189 5.47656C12.932 5.50007 12.945 5.52358 12.9551 5.54883C12.9606 5.56278 12.9633 5.57756 12.9678 5.5918C12.9757 5.61679 12.9836 5.64173 12.9883 5.66797C12.9905 5.68025 12.9907 5.69271 12.9922 5.70508C12.9951 5.72947 13 5.75413 13 5.7793V6.94629C12.9999 7.29129 12.72 7.57119 12.375 7.57129C12.1161 7.57129 11.8937 7.41377 11.7988 7.18945L10.0713 8.83887C9.6505 9.24037 8.98076 9.23948 8.56055 8.83887L7.68457 8.00293L5.4668 10.1211C5.21706 10.3593 4.82128 10.3493 4.58301 10.0996C4.34544 9.84993 4.35426 9.45491 4.60352 9.2168L6.92969 6.99707L7.01172 6.92676C7.43295 6.59806 8.0459 6.62097 8.44043 6.99707L9.31543 7.83203L10.8799 6.33887C10.6715 6.2376 10.5265 6.02657 10.5264 5.7793C10.5264 5.43412 10.8062 5.1543 11.1514 5.1543H12.3887C12.4215 5.15505 12.454 5.15917 12.4863 5.16504Z"),
            )
        }.build()
        return _ic_chart_ascending_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartAscending16Preview() {
    Icon(
        imageVector = Icons.ic_chart_ascending_16,
        contentDescription = null,
    )
}