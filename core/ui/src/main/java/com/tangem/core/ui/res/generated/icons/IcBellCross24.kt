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

private var _ic_bell_cross_24: ImageVector? = null

val Icons.ic_bell_cross_24: ImageVector
    get() {
        if (_ic_bell_cross_24 != null) return _ic_bell_cross_24!!
        _ic_bell_cross_24 = ImageVector.Builder(
            name = "ic_bell_cross_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.39043 3.36182C3.78092 2.97139 4.41397 2.97138 4.80449 3.36182L20.6736 19.228C21.064 19.6185 21.0649 20.2516 20.6746 20.6421C20.2842 21.0326 19.6501 21.0324 19.2596 20.6421L17.616 18.9985H15.4871C15.3818 19.7438 15.0372 20.4399 14.4988 20.978C13.8443 21.6321 12.9563 21.9979 12.0311 21.9976C11.1064 21.9978 10.2185 21.6325 9.56425 20.979C9.02576 20.4408 8.68203 19.744 8.57695 18.9985H6.08574C5.27631 18.9985 4.5 18.6772 3.92754 18.105C3.35518 17.5326 3.03314 16.7562 3.033 15.9468L3.03691 15.7886C3.07633 15.0055 3.40441 14.2622 3.96074 13.7046L5.033 12.6323V6.41748L3.39043 4.77588C2.99994 4.38539 2.99997 3.75236 3.39043 3.36182ZM10.6268 18.9985C10.6999 19.2091 10.8178 19.4035 10.9783 19.564C11.2574 19.8426 11.6367 19.9987 12.0311 19.9985C12.4259 19.9986 12.8055 19.8421 13.0848 19.563C13.2453 19.4025 13.3641 19.2089 13.4373 18.9985H10.6268ZM7.033 13.0474C7.03298 13.3125 6.92743 13.5679 6.74004 13.7554L5.37578 15.1177C5.15655 15.3378 5.03343 15.6361 5.033 15.9468L5.03789 16.0503C5.06181 16.2912 5.16888 16.5182 5.3416 16.6909C5.53898 16.8881 5.80672 16.9985 6.08574 16.9985H15.615L7.033 8.41748V13.0474Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.032 2.00244C15.8974 2.00265 19.0311 5.1361 19.0311 9.00146V12.7046C19.0308 13.2566 18.5831 13.7045 18.0311 13.7046C17.479 13.7045 17.0313 13.2566 17.0311 12.7046V9.00146C17.0311 6.24078 14.793 4.00265 12.032 4.00244C11.0879 4.00246 10.2074 4.26346 9.45586 4.71631C8.98286 5.00129 8.36787 4.84845 8.08281 4.37549C7.79808 3.90267 7.95012 3.28856 8.42265 3.00342C9.4771 2.36794 10.7139 2.00246 12.032 2.00244Z"),
            )
        }.build()
        return _ic_bell_cross_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBellCross24Preview() {
    Icon(
        imageVector = Icons.ic_bell_cross_24,
        contentDescription = null,
    )
}