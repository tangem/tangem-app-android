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

private var _ic_document_24: ImageVector? = null

val Icons.ic_document_24: ImageVector
    get() {
        if (_ic_document_24 != null) return _ic_document_24!!
        _ic_document_24 = ImageVector.Builder(
            name = "ic_document_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15 15.5C15.5523 15.5 16 15.9477 16 16.5C16 17.0523 15.5523 17.5 15 17.5H9C8.44772 17.5 8 17.0523 8 16.5C8 15.9477 8.44772 15.5 9 15.5H15Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11 12C11.5523 12 12 12.4477 12 13C12 13.5523 11.5523 14 11 14H9C8.44772 14 8 13.5523 8 13C8 12.4477 8.44772 12 9 12H11Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.7578 2C15.8185 2.00012 16.8359 2.42184 17.5859 3.17188L19.8281 5.41406C20.5782 6.1641 20.9999 7.18148 21 8.24219V18C21 20.2091 19.2091 22 17 22H7C4.79086 22 3 20.2091 3 18V6C3 3.79086 4.79086 2 7 2H14.7578ZM7 4C5.89543 4 5 4.89543 5 6V18C5 19.1046 5.89543 20 7 20H17C18.1046 20 19 19.1046 19 18V9.5H16.5C14.8431 9.5 13.5 8.15685 13.5 6.5V4H7ZM15.5 6.5C15.5 7.05228 15.9477 7.5 16.5 7.5H18.8564C18.7568 7.25077 18.6075 7.02155 18.4141 6.82812L16.1719 4.58594C15.9785 4.39252 15.7492 4.24324 15.5 4.14355V6.5Z"),
            )
        }.build()
        return _ic_document_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDocument24Preview() {
    Icon(
        imageVector = Icons.ic_document_24,
        contentDescription = null,
    )
}