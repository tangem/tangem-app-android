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

private var _ic_share_ios_20: ImageVector? = null

val Icons.ic_share_ios_20: ImageVector
    get() {
        if (_ic_share_ios_20 != null) return _ic_share_ios_20!!
        _ic_share_ios_20 = ImageVector.Builder(
            name = "ic_share_ios_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.17578 6.97168C7.5897 6.97184 7.92553 7.30778 7.92578 7.72168C7.92578 8.13579 7.58985 8.47151 7.17578 8.47168H6.42285C5.91686 8.47181 5.50612 8.88271 5.50586 9.38867V15.0801C5.50586 15.5863 5.9167 15.9969 6.42285 15.9971H13.5762C14.0823 15.9969 14.4932 15.5862 14.4932 15.0801V9.38867C14.4929 8.88273 14.0821 8.47184 13.5762 8.47168H12.8232C12.4091 8.47155 12.0732 8.13581 12.0732 7.72168C12.0735 7.30776 12.4093 6.97181 12.8232 6.97168H13.5762C14.9106 6.97184 15.9929 8.0543 15.9932 9.38867V15.0801C15.9932 16.4147 14.9107 17.4969 13.5762 17.4971H6.42285C5.08827 17.4969 4.00585 16.4147 4.00586 15.0801V9.38867C4.00612 8.05429 5.08844 6.97181 6.42285 6.97168H7.17578Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99902 1.99512C10.1977 1.99514 10.3887 2.0745 10.5293 2.21484L12.5322 4.21777C12.825 4.51059 12.8248 4.98543 12.5322 5.27832C12.2393 5.57107 11.7645 5.5712 11.4717 5.27832L10.749 4.55566V11.1465C10.749 11.5606 10.4132 11.8965 9.99902 11.8965C9.58507 11.8962 9.24909 11.5605 9.24902 11.1465V4.55566L8.52637 5.27832C8.23349 5.57112 7.7587 5.57115 7.46582 5.27832C7.17362 4.98544 7.1733 4.51048 7.46582 4.21777L9.46875 2.21484L9.58301 2.12109C9.70514 2.03969 9.85009 1.99523 9.99902 1.99512Z"),
            )
        }.build()
        return _ic_share_ios_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShareIos20Preview() {
    Icon(
        imageVector = Icons.ic_share_ios_20,
        contentDescription = null,
    )
}