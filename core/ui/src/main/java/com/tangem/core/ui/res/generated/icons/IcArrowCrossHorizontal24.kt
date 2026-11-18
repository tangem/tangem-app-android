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

private var _ic_arrow_cross_horizontal_24: ImageVector? = null

val Icons.ic_arrow_cross_horizontal_24: ImageVector
    get() {
        if (_ic_arrow_cross_horizontal_24 != null) return _ic_arrow_cross_horizontal_24!!
        _ic_arrow_cross_horizontal_24 = ImageVector.Builder(
            name = "ic_arrow_cross_horizontal_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.7998 13.7002C12.2416 13.3688 12.8688 13.4585 13.2002 13.9004L14.0996 15.0996C14.532 15.6761 15.1485 15.9999 15.7998 16H18.5859L18.293 15.707C17.9024 15.3165 17.9024 14.6835 18.293 14.293C18.6835 13.9024 19.3165 13.9024 19.707 14.293L21.707 16.293C22.0976 16.6835 22.0976 17.3165 21.707 17.707L19.707 19.707C19.3165 20.0975 18.6835 20.0975 18.293 19.707C17.9024 19.3165 17.9024 18.6835 18.293 18.293L18.5859 18H15.7998C14.4513 17.9999 13.2675 17.3232 12.5 16.2998L11.5996 15.0996C11.2687 14.6579 11.3583 14.0315 11.7998 13.7002ZM18.293 4.29295C18.6835 3.90243 19.3165 3.90243 19.707 4.29295L21.707 6.29295C22.0976 6.68348 22.0976 7.31649 21.707 7.70702L19.707 9.70702C19.3165 10.0975 18.6835 10.0975 18.293 9.70702C17.9024 9.31649 17.9024 8.68348 18.293 8.29295L18.5859 7.99999H15.7998C15.1485 8.00005 14.532 8.32387 14.0996 8.90038L14.0977 8.90331L8.49707 16.3037C7.72945 17.325 6.54706 17.9999 5.2002 18H3C2.44772 18 2 17.5523 2 17C2 16.4477 2.44772 16 3 16H5.2002C5.85151 15.9999 6.46801 15.6761 6.90039 15.0996L6.90234 15.0967L12.5029 7.69627C13.2705 6.675 14.4529 6.00005 15.7998 5.99999H18.5859L18.293 5.70702C17.9024 5.31649 17.9024 4.68348 18.293 4.29295ZM5.2002 5.99999C6.54872 6.00005 7.73246 6.6768 8.5 7.70018L9.40039 8.90038C9.73132 9.34211 9.64169 9.96849 9.2002 10.2998C8.75837 10.6312 8.13118 10.5414 7.7998 10.0996L6.90039 8.90038C6.46801 8.32387 5.85151 8.00005 5.2002 7.99999H3C2.44772 7.99999 2 7.55227 2 6.99999C2 6.4477 2.44772 5.99999 3 5.99999H5.2002Z"),
            )
        }.build()
        return _ic_arrow_cross_horizontal_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowCrossHorizontal24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_cross_horizontal_24,
        contentDescription = null,
    )
}