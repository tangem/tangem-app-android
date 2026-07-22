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

private var _ic_error_28_filled: ImageVector? = null

val Icons.ic_error_28_filled: ImageVector
    get() {
        if (_ic_error_28_filled != null) return _ic_error_28_filled!!
        _ic_error_28_filled = ImageVector.Builder(
            name = "ic_error_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.79 2C18.755 2.00008 19.6812 2.38432 20.3633 3.06641L24.9346 7.6377C25.6187 8.3202 26.0039 9.24778 26.0039 10.2129V17.79C26.0038 18.6945 25.6665 19.5652 25.0625 20.2324L24.9375 20.3633L19.665 25.6377C19.4307 25.8721 19.1117 26.0038 18.7803 26.0039H10.2129C9.24798 26.0039 8.32177 25.6195 7.63965 24.9375L3.06641 20.3643C2.38434 19.6822 2.00007 18.756 2 17.791V10.2129C2 9.24784 2.38428 8.3218 3.06641 7.63965L7.63965 3.06641C8.3218 2.38429 9.24785 2 10.2129 2H17.79ZM14.1572 17.3887L14 17.3809C13.1549 17.3813 12.4604 18.0663 12.4619 18.9209L12.4697 19.0781C12.5487 19.854 13.2054 20.4597 14.002 20.46C14.8515 20.4596 15.5407 19.7705 15.541 18.9209C15.541 18.1209 14.932 17.4672 14.1572 17.3887ZM15.252 8.79395C15.2519 8.10384 14.692 7.54431 14.002 7.54395C13.3118 7.54421 12.752 8.10378 12.752 8.79395V14.5811C12.7524 15.2709 13.3121 15.8308 14.002 15.8311C14.6917 15.8307 15.2515 15.2708 15.252 14.5811V8.79395Z"),
            )
        }.build()
        return _ic_error_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcError28FilledPreview() {
    Icon(
        imageVector = Icons.ic_error_28_filled,
        contentDescription = null,
    )
}