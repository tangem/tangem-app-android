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

private var _ic_sack_28: ImageVector? = null

val Icons.ic_sack_28: ImageVector
    get() {
        if (_ic_sack_28 != null) return _ic_sack_28!!
        _ic_sack_28 = ImageVector.Builder(
            name = "ic_sack_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.0118 2.00195C18.8115 2.00198 19.5572 2.39277 20.0127 3.04199L20.0996 3.1748L20.1778 3.31348C20.5453 4.01599 20.5504 4.85799 20.1836 5.56836L19.9795 5.96289C20.724 6.41616 21.2227 7.23356 21.2227 8.16895V10.4199C21.2225 10.7066 21.1244 10.9698 20.962 11.1807L22.6905 13.1318C24.9684 15.7041 25.4544 19.4033 23.918 22.4766L23.8204 22.6709C22.7993 24.7131 20.712 26.0038 18.4287 26.0039H9.585C7.30166 26.0039 5.21449 24.7132 4.1934 22.6709L4.08207 22.4492C2.55363 19.3922 3.02575 15.7138 5.27641 13.1416L7.01664 11.1523C6.86702 10.9464 6.77755 10.6939 6.77738 10.4199V8.16895C6.77738 7.23392 7.2754 6.41626 8.01957 5.96289L7.81644 5.56836C7.42526 4.81064 7.45693 3.90312 7.90043 3.1748L7.98832 3.04199C8.44388 2.39302 9.18878 2.00202 9.98832 2.00195H18.0118ZM7.15727 14.7881C5.57517 16.5963 5.24397 19.182 6.3184 21.3311L6.42973 21.5537C7.02745 22.7487 8.24882 23.5039 9.585 23.5039H18.4287C19.7649 23.5038 20.9864 22.7487 21.584 21.5537L21.6817 21.3584C22.7616 19.198 22.4207 16.5973 20.8194 14.7891L18.0567 11.6699H9.88676L7.15727 14.7881ZM9.36039 8.08594C9.31467 8.08594 9.27738 8.12323 9.27738 8.16895V9.16992H18.7227V8.16895C18.7227 8.12341 18.6861 8.08623 18.6407 8.08594H9.36039ZM10.6397 5.58594H17.3614L17.9209 4.50195H10.0801L10.6397 5.58594Z"),
            )
        }.build()
        return _ic_sack_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSack28Preview() {
    Icon(
        imageVector = Icons.ic_sack_28,
        contentDescription = null,
    )
}