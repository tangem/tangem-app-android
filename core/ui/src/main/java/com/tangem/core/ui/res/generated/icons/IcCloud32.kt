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

private var _ic_cloud_32: ImageVector? = null

val Icons.ic_cloud_32: ImageVector
    get() {
        if (_ic_cloud_32 != null) return _ic_cloud_32!!
        _ic_cloud_32 = ImageVector.Builder(
            name = "ic_cloud_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.999 6C20.4737 6 24.2291 9.20464 24.8936 13.4404C27.7926 14.0624 29.9968 16.5812 29.9971 19.6459C29.9971 23.1868 27.0549 26 23.498 26H9.75098C5.50398 25.9999 2.00195 22.6425 2.00195 18.4304V18.4275L2.01074 18.0729C2.17591 14.7252 4.57611 11.9315 7.82422 11.1251C9.28085 8.0162 12.4715 6.00178 15.999 6ZM15.999 8.9847C13.4111 8.98653 11.1394 10.5887 10.3223 12.9264C10.1359 13.4597 9.66324 13.8431 9.10059 13.9164C6.7311 14.2248 5.0063 16.1764 5.00195 18.4333C5.00347 20.9321 7.09697 23.0152 9.75098 23.0153H23.498C25.4629 23.0153 26.9971 21.4747 26.9971 19.6459C26.9968 17.8173 25.4627 16.2764 23.498 16.2764C22.6697 16.2763 21.998 15.6082 21.998 14.7841C21.9976 11.6132 19.344 8.9847 15.999 8.9847Z"),
            )
        }.build()
        return _ic_cloud_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud32Preview() {
    Icon(
        imageVector = Icons.ic_cloud_32,
        contentDescription = null,
    )
}