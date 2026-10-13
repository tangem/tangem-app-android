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

private var _ic_mask_20: ImageVector? = null

val Icons.ic_mask_20: ImageVector
    get() {
        if (_ic_mask_20 != null) return _ic_mask_20!!
        _ic_mask_20 = ImageVector.Builder(
            name = "ic_mask_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 4.45219C16.0019 4.14891 15.8614 3.86513 15.626 3.68656C15.3913 3.50858 15.0883 3.455 14.8096 3.5391C11.6716 4.48109 8.32946 4.48111 5.19143 3.5391C4.91268 3.45498 4.60881 3.50856 4.37405 3.68656C4.13864 3.86513 3.99812 4.14891 4.00002 4.45219V10.4239C4.00002 13.7875 6.69422 16.5 10 16.5C13.3058 16.5 16 13.7875 16 10.4239V4.45219ZM11.5889 11.5899C11.8872 11.3027 12.3621 11.3121 12.6494 11.6104C12.9368 11.9087 12.9282 12.3836 12.6299 12.6709C11.1582 14.0883 8.84184 14.0883 7.37014 12.6709C7.07184 12.3836 7.06329 11.9087 7.35061 11.6104C7.63791 11.3121 8.11281 11.3027 8.41116 11.5899C9.30196 12.4478 10.6981 12.4478 11.5889 11.5899ZM8.31252 7.1143C8.72672 7.1143 9.0625 7.4501 9.06252 7.8643C9.06252 8.27851 8.72674 8.6143 8.31252 8.6143H6.62502C6.21082 8.61428 5.87502 8.2785 5.87502 7.8643C5.87505 7.45011 6.21084 7.11431 6.62502 7.1143H8.31252ZM13.375 7.1143C13.7892 7.1143 14.125 7.4501 14.125 7.8643C14.125 8.27851 13.7892 8.6143 13.375 8.6143H11.6875C11.2733 8.61429 10.9375 8.27851 10.9375 7.8643C10.9375 7.45011 11.2733 7.11431 11.6875 7.1143H13.375ZM17.5 10.4239C17.5 14.6 14.15 18 10 18C5.85002 18 2.50002 14.6 2.50002 10.4239V4.45707C2.49657 3.68688 2.85391 2.95791 3.4678 2.49223C4.08341 2.02525 4.8832 1.87943 5.62307 2.10258L5.6221 2.10356C8.47867 2.96112 11.5204 2.96093 14.377 2.10356C15.1169 1.88039 15.9166 2.02525 16.5323 2.49223C17.1461 2.95791 17.5035 3.68688 17.5 4.45707V10.4239Z"),
            )
        }.build()
        return _ic_mask_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMask20Preview() {
    Icon(
        imageVector = Icons.ic_mask_20,
        contentDescription = null,
    )
}