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

private var _ic_search_16: ImageVector? = null

val Icons.ic_search_16: ImageVector
    get() {
        if (_ic_search_16 != null) return _ic_search_16!!
        _ic_search_16 = ImageVector.Builder(
            name = "ic_search_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.49316 2.99902C9.97494 2.99922 11.9873 5.01131 11.9873 7.49316C11.9873 8.50867 11.6492 9.44469 11.0811 10.1973L12.8164 11.9346C13.0603 12.1788 13.0606 12.5754 12.8164 12.8193C12.5722 13.0633 12.1756 13.0626 11.9316 12.8184L10.1973 11.0811C9.44467 11.6492 8.50863 11.9872 7.49316 11.9873C5.01126 11.9873 2.9991 9.97508 2.99902 7.49316C2.99902 5.01119 5.01122 2.99902 7.49316 2.99902ZM7.49316 4.24902C5.70161 4.24902 4.24902 5.70151 4.24902 7.49316C4.2491 9.28476 5.70166 10.7373 7.49316 10.7373C9.28451 10.7371 10.7372 9.28464 10.7373 7.49316C10.7373 5.70163 9.28455 4.24922 7.49316 4.24902Z"),
            )
        }.build()
        return _ic_search_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSearch16Preview() {
    Icon(
        imageVector = Icons.ic_search_16,
        contentDescription = null,
    )
}