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

private var _ic_arrow_up_down_20: ImageVector? = null

val Icons.ic_arrow_up_down_20: ImageVector
    get() {
        if (_ic_arrow_up_down_20 != null) return _ic_arrow_up_down_20!!
        _ic_arrow_up_down_20 = ImageVector.Builder(
            name = "ic_arrow_up_down_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.3936 3.20996C6.43713 3.20996 6.48003 3.21456 6.52153 3.22168C6.673 3.24798 6.81486 3.31966 6.92485 3.42969L9.82426 6.33105C10.1166 6.62403 10.117 7.09893 9.82426 7.3916C9.53155 7.68413 9.05659 7.68381 8.76372 7.3916L7.1436 5.77051V16.042C7.14335 16.456 6.80766 16.792 6.3936 16.792C5.97969 16.7918 5.64385 16.4559 5.6436 16.042V5.77051L4.02348 7.3916C3.73084 7.6842 3.25588 7.68373 2.96294 7.3916C2.67006 7.09872 2.67008 6.62395 2.96294 6.33105L5.86333 3.42969C5.9706 3.32243 6.10692 3.2515 6.25395 3.22363C6.29921 3.21509 6.34586 3.20998 6.3936 3.20996Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.6055 3.20703C14.0194 3.20703 14.355 3.54326 14.3555 3.95703V14.2314L15.9776 12.6094C16.2704 12.3171 16.7454 12.317 17.0381 12.6094C17.3309 12.9021 17.3306 13.377 17.0381 13.6699L14.1377 16.5713C14.0229 16.6861 13.8737 16.757 13.7149 16.7803C13.6876 16.7843 13.66 16.7871 13.6319 16.7881C13.6237 16.7884 13.6157 16.791 13.6075 16.791C13.6004 16.791 13.5931 16.7883 13.586 16.7881C13.5542 16.7873 13.5229 16.7849 13.4922 16.7803C13.3357 16.756 13.1894 16.6845 13.0762 16.5713L10.1768 13.6709C9.88407 13.378 9.88397 12.9032 10.1768 12.6104C10.4696 12.3177 10.9445 12.3168 11.2373 12.6094L12.8555 14.2275V3.95801C12.8558 3.54415 13.1916 3.20725 13.6055 3.20703Z"),
            )
        }.build()
        return _ic_arrow_up_down_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUpDown20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_down_20,
        contentDescription = null,
    )
}