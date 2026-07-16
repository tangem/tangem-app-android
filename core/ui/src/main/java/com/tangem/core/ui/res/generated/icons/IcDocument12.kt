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

private var _ic_document_12: ImageVector? = null

val Icons.ic_document_12: ImageVector
    get() {
        if (_ic_document_12 != null) return _ic_document_12!!
        _ic_document_12 = ImageVector.Builder(
            name = "ic_document_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.5 7.75C7.77614 7.75 8 7.97386 8 8.25C8 8.52614 7.77614 8.75 7.5 8.75H4.5C4.22386 8.75 4 8.52614 4 8.25C4 7.97386 4.22386 7.75 4.5 7.75H7.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.5 5.75C5.77614 5.75 6 5.97386 6 6.25C6 6.52614 5.77614 6.75 5.5 6.75H4.5C4.22386 6.75 4 6.52614 4 6.25C4 5.97386 4.22386 5.75 4.5 5.75H5.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.37891 1C7.90926 1.00006 8.41795 1.21092 8.79297 1.58594L9.91406 2.70703C10.2891 3.08205 10.4999 3.59074 10.5 4.12109V9C10.5 10.1046 9.60457 11 8.5 11H3.5C2.39543 11 1.5 10.1046 1.5 9V3C1.5 1.89543 2.39543 1 3.5 1H7.37891ZM3.5 2C2.94772 2 2.5 2.44772 2.5 3V9C2.5 9.55229 2.94772 10 3.5 10H8.5C9.05228 10 9.5 9.55229 9.5 9V4.75H8.25C7.42157 4.75 6.75 4.07843 6.75 3.25V2H3.5ZM7.75 3.25C7.75 3.52614 7.97386 3.75 8.25 3.75H9.42773C9.37789 3.62545 9.3037 3.51073 9.20703 3.41406L8.08594 2.29297C7.98918 2.19622 7.87468 2.12113 7.75 2.07129V3.25Z"),
            )
        }.build()
        return _ic_document_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDocument12Preview() {
    Icon(
        imageVector = Icons.ic_document_12,
        contentDescription = null,
    )
}