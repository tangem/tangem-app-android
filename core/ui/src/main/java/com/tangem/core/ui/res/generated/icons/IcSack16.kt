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

private var _ic_sack_16: ImageVector? = null

val Icons.ic_sack_16: ImageVector
    get() {
        if (_ic_sack_16 != null) return _ic_sack_16!!
        _ic_sack_16 = ImageVector.Builder(
            name = "ic_sack_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.0051 2C10.4312 2.00028 10.8265 2.22307 11.0481 2.58691C11.2694 2.95074 11.2851 3.40364 11.09 3.78223L10.9875 3.98047C11.3593 4.20709 11.6085 4.61574 11.6086 5.08301V6.20898C11.6085 6.35181 11.5575 6.48163 11.4768 6.58691L12.342 7.56543C13.4797 8.85136 13.7227 10.7003 12.9553 12.2363L12.9065 12.334C12.3964 13.3545 11.3538 13.9996 10.2131 14H5.79415C4.65333 13.9999 3.60998 13.3545 3.09981 12.334L3.04512 12.2227C2.28164 10.6946 2.51658 8.85621 3.64083 7.57031L4.51094 6.5752C4.43595 6.47211 4.39095 6.34612 4.39083 6.20898V5.08301C4.39096 4.61548 4.63974 4.20701 5.01192 3.98047L4.91036 3.7832C4.71528 3.40457 4.73186 2.95079 4.95333 2.58691C5.17502 2.22312 5.57007 2.00004 5.9963 2H10.0051ZM4.58223 8.39355C3.79199 9.29751 3.62646 10.5897 4.16329 11.6641L4.21798 11.7754C4.51665 12.3724 5.12735 12.7499 5.79415 12.75H10.2131C10.8798 12.7496 11.4898 12.3725 11.7883 11.7754L11.8371 11.6777C12.3767 10.5978 12.2061 9.2975 11.4065 8.39355L10.0256 6.83398H5.94551L4.58223 8.39355ZM5.68184 5.04199C5.65928 5.04219 5.64103 5.06045 5.64083 5.08301V5.58398H10.3586V5.08301C10.3584 5.06056 10.34 5.04237 10.3176 5.04199H5.68184ZM6.32149 3.79199H9.67989L9.95919 3.25H6.04219L6.32149 3.79199Z"),
            )
        }.build()
        return _ic_sack_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSack16Preview() {
    Icon(
        imageVector = Icons.ic_sack_16,
        contentDescription = null,
    )
}