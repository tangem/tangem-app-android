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

private var _ic_document_20: ImageVector? = null

val Icons.ic_document_20: ImageVector
    get() {
        if (_ic_document_20 != null) return _ic_document_20!!
        _ic_document_20 = ImageVector.Builder(
            name = "ic_document_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.3438 12.8779C12.758 12.8779 13.0937 13.2137 13.0938 13.6279C13.0935 14.0419 12.7578 14.3779 12.3438 14.3779H7.65625C7.24229 14.3778 6.90649 14.0419 6.90625 13.6279C6.90627 13.2138 7.24216 12.8781 7.65625 12.8779H12.3438Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.21973 10.1387C9.63394 10.1387 9.96973 10.4745 9.96973 10.8887C9.96927 11.3025 9.63366 11.6387 9.21973 11.6387H7.65625C7.24243 11.6385 6.90671 11.3024 6.90625 10.8887C6.90625 10.4745 7.24215 10.1388 7.65625 10.1387H9.21973Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1543 2C12.9823 2.00007 13.7723 2.33929 14.3506 2.93555L16.1025 4.74316C16.6799 5.33876 17.001 6.14209 17.001 6.97461V14.8369C17.0006 16.5636 15.637 18.0036 13.9072 18.0039H6.09375C4.36374 18.0038 3.00043 16.5638 3 14.8369V5.16699C3.00017 3.4399 4.36358 2.00008 6.09375 2H12.1543ZM6.09375 3.5C5.23526 3.50008 4.50016 4.2244 4.5 5.16699V14.8369C4.50042 15.7793 5.23541 16.5038 6.09375 16.5039H13.9072C14.7654 16.5036 15.5006 15.7791 15.501 14.8369V7.93164H13.5166C12.218 7.93159 11.2044 6.85231 11.2041 5.57031V3.5H6.09375ZM12.7041 5.57031C12.7044 6.06777 13.0896 6.43158 13.5166 6.43164H15.4121C15.3323 6.18985 15.2012 5.96857 15.0254 5.78711L13.2734 3.98047C13.1094 3.81133 12.9143 3.68453 12.7041 3.60352V5.57031Z"),
            )
        }.build()
        return _ic_document_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDocument20Preview() {
    Icon(
        imageVector = Icons.ic_document_20,
        contentDescription = null,
    )
}