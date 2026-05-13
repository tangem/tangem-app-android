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

private var _ic_loading_spinner_12: ImageVector? = null

val Icons.ic_loading_spinner_12: ImageVector
    get() {
        if (_ic_loading_spinner_12 != null) return _ic_loading_spinner_12!!
        _ic_loading_spinner_12 = ImageVector.Builder(
            name = "ic_loading_spinner_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6 1C6.98891 1 7.95607 1.29337 8.77832 1.84277C9.60038 2.39217 10.2408 3.17342 10.6191 4.08691C10.9975 5.00049 11.0972 6.00575 10.9043 6.97559C10.7114 7.94547 10.2344 8.83591 9.53516 9.53516C8.83591 10.2344 7.94547 10.7114 6.97559 10.9043C6.00575 11.0972 5.00049 10.9975 4.08691 10.6191C3.17342 10.2408 2.39217 9.60038 1.84277 8.77832C1.29337 7.95607 1 6.98891 1 6C1 5.72386 1.22386 5.5 1.5 5.5C1.77614 5.5 2 5.72386 2 6C2 6.79113 2.2343 7.56486 2.67383 8.22266C3.11335 8.88039 3.73887 9.39258 4.46973 9.69531C5.20054 9.99795 6.00447 10.0772 6.78027 9.92285C7.5562 9.76851 8.26872 9.38754 8.82812 8.82812C9.38754 8.26871 9.76851 7.5562 9.92285 6.78027C10.0772 6.00447 9.99795 5.20054 9.69531 4.46973C9.39258 3.73887 8.88039 3.11335 8.22266 2.67383C7.56486 2.2343 6.79113 2 6 2C5.72386 2 5.5 1.77614 5.5 1.5C5.5 1.22386 5.72386 1 6 1Z"),
            )
        }.build()
        return _ic_loading_spinner_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLoadingSpinner12Preview() {
    Icon(
        imageVector = Icons.ic_loading_spinner_12,
        contentDescription = null,
    )
}