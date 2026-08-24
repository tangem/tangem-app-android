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

private var _ic_cards_star_20: ImageVector? = null

val Icons.ic_cards_star_20: ImageVector
    get() {
        if (_ic_cards_star_20 != null) return _ic_cards_star_20!!
        _ic_cards_star_20 = ImageVector.Builder(
            name = "ic_cards_star_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.4688 6.47266C12.883 6.47266 13.2188 6.80844 13.2188 7.22266C13.2189 7.57458 13.5046 7.85996 13.8565 7.86035C14.2707 7.86035 14.6065 8.19614 14.6065 8.61035C14.6065 9.02453 14.2707 9.36035 13.8565 9.36035C13.5046 9.36074 13.2188 9.64605 13.2188 9.99805C13.2188 10.4123 12.883 10.748 12.4688 10.748C12.0549 10.7477 11.7188 10.4121 11.7188 9.99805C11.7188 9.8289 11.6519 9.66648 11.5323 9.54688C11.4127 9.42742 11.2501 9.36035 11.0811 9.36035C10.6671 9.36011 10.3311 9.02438 10.3311 8.61035C10.3311 8.22234 10.6262 7.90309 11.004 7.86426L11.0811 7.86035L11.21 7.84766C11.5005 7.78809 11.7187 7.53069 11.7188 7.22266C11.7188 6.80864 12.0549 6.47297 12.4688 6.47266Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.8672 3.00098C16.0446 3.00098 17 3.95543 17 5.13281V12.0879C17 13.2653 16.0446 14.2197 14.8672 14.2197H13.7774C13.7363 14.3811 13.6738 14.5379 13.5889 14.6846C13.3462 15.1036 12.9465 15.4084 12.4786 15.5332L7.26567 16.9365C6.28979 17.1971 5.28578 16.6195 5.02153 15.6445L5.02056 15.6426L3.06255 8.33984V8.33691C2.80526 7.36261 3.38084 6.36229 4.35259 6.0957L4.35747 6.09473L8.00005 5.12305C8.00534 3.9503 8.95796 3.00122 10.1319 3.00098H14.8672ZM4.74809 7.54199L4.74907 7.54297C4.57134 7.59203 4.46601 7.77477 4.51274 7.95312L6.4688 15.252C6.51675 15.4289 6.69884 15.5338 6.87602 15.4873L11.5889 14.2197H10.1319C8.95468 14.2195 8.00005 13.2651 8.00005 12.0879V6.6748L4.74809 7.54199ZM10.1319 4.50098C9.78313 4.50122 9.50008 4.78401 9.50005 5.13281V12.0879C9.50005 12.4367 9.78311 12.7195 10.1319 12.7197H14.8672C15.2162 12.7197 15.5 12.4369 15.5 12.0879V5.13281C15.5 4.78385 15.2162 4.50098 14.8672 4.50098H10.1319Z"),
            )
        }.build()
        return _ic_cards_star_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCardsStar20Preview() {
    Icon(
        imageVector = Icons.ic_cards_star_20,
        contentDescription = null,
    )
}