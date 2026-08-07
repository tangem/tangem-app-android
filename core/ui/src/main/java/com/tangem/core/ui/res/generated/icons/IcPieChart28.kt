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

private var _ic_pie_chart_28: ImageVector? = null

val Icons.ic_pie_chart_28: ImageVector
    get() {
        if (_ic_pie_chart_28 != null) return _ic_pie_chart_28!!
        _ic_pie_chart_28 = ImageVector.Builder(
            name = "ic_pie_chart_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2C20.6279 2 26 7.37206 26 14C26 20.6279 20.6279 26 14 26C7.37206 26 2 20.6279 2 14C2 7.37206 7.37206 2 14 2ZM12.75 4.58398C8.09382 5.19597 4.5 9.17661 4.5 14C4.5 19.2472 8.75277 23.5 14 23.5C16.1729 23.5 18.174 22.7691 19.7744 21.542L13.4658 15.2334C13.2624 15.0299 13.1007 14.7935 12.9814 14.5381C12.9379 14.4563 12.9038 14.3721 12.8799 14.2861C12.7957 14.0368 12.75 13.7739 12.75 13.5059V4.58398ZM16.0352 14.2676L21.542 19.7744C22.7691 18.174 23.5 16.1729 23.5 14C23.5 12.7141 23.2431 11.4886 22.7803 10.3701L16.0352 14.2676ZM15.25 11.835L21.5283 8.20703C20.0315 6.26458 17.8006 4.91923 15.25 4.58398V11.835Z"),
            )
        }.build()
        return _ic_pie_chart_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPieChart28Preview() {
    Icon(
        imageVector = Icons.ic_pie_chart_28,
        contentDescription = null,
    )
}