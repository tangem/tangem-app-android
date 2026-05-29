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

private var _ic_gauge_24: ImageVector? = null

val Icons.ic_gauge_24: ImageVector
    get() {
        if (_ic_gauge_24 != null) return _ic_gauge_24!!
        _ic_gauge_24 = ImageVector.Builder(
            name = "ic_gauge_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.93776 4.85889C8.84101 1.04712 15.1596 1.04695 19.0628 4.85889C22.979 8.68379 22.9789 14.8952 19.0628 18.7202C18.6677 19.106 18.0345 19.0986 17.6487 18.7036C17.263 18.3086 17.2704 17.6754 17.6653 17.2896C20.778 14.2492 20.7782 9.32975 17.6653 6.28956C14.5392 3.23657 9.46141 3.23674 6.33522 6.28956C3.22225 9.32984 3.22229 14.2492 6.33522 17.2896C6.72999 17.6754 6.73731 18.3086 6.35182 18.7036C5.96597 19.0986 5.33286 19.106 4.93776 18.7202C1.0214 14.8952 1.0214 8.68387 4.93776 4.85889Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.596 7.68702C14.9911 7.30184 15.6244 7.30981 16.01 7.70459C16.3954 8.09963 16.3881 8.73287 15.9934 9.11866L12.6116 12.4204C12.2164 12.8058 11.5832 12.7988 11.1975 12.4038C10.8119 12.0088 10.8203 11.3756 11.2151 10.9897L14.596 7.68702Z"),
            )
        }.build()
        return _ic_gauge_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGauge24Preview() {
    Icon(
        imageVector = Icons.ic_gauge_24,
        contentDescription = null,
    )
}