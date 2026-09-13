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

private var _ic_wallet_checkmark_16: ImageVector? = null

val Icons.ic_wallet_checkmark_16: ImageVector
    get() {
        if (_ic_wallet_checkmark_16 != null) return _ic_wallet_checkmark_16!!
        _ic_wallet_checkmark_16 = ImageVector.Builder(
            name = "ic_wallet_checkmark_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.7646 4.1499C11.7458 4.15003 12.5419 4.94609 12.542 5.92725V6.42236C12.8201 6.61898 13.0019 6.94264 13.002 7.30908V8.69092C13.002 9.05715 12.8198 9.37995 12.542 9.57666V10.0737C12.5419 11.0549 11.7458 11.85 10.7646 11.8501H4.77539C3.79409 11.8501 2.99812 11.055 2.99805 10.0737V5.92725C2.99818 4.94601 3.79412 4.1499 4.77539 4.1499H10.7646ZM4.77539 5.3999C4.48448 5.3999 4.24818 5.63636 4.24805 5.92725V10.0737C4.24812 10.3647 4.48444 10.6001 4.77539 10.6001H10.7646C11.0555 10.6 11.2919 10.3646 11.292 10.0737V9.77686H10.7646C9.78356 9.77672 8.98755 8.98155 8.9873 8.00049C8.98744 7.01933 9.78349 6.22328 10.7646 6.22314H11.292V5.92725C11.2919 5.63645 11.0554 5.40003 10.7646 5.3999H4.77539ZM10.7646 7.47314C10.4738 7.47328 10.2374 7.70969 10.2373 8.00049C10.2375 8.29119 10.4739 8.52672 10.7646 8.52686H11.752V7.47314H10.7646Z"),
            )
        }.build()
        return _ic_wallet_checkmark_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletCheckmark16Preview() {
    Icon(
        imageVector = Icons.ic_wallet_checkmark_16,
        contentDescription = null,
    )
}