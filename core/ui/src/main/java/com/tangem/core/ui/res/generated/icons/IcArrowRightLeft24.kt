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

private var _ic_arrow_right_left_24: ImageVector? = null

val Icons.ic_arrow_right_left_24: ImageVector
    get() {
        if (_ic_arrow_right_left_24 != null) return _ic_arrow_right_left_24!!
        _ic_arrow_right_left_24 = ImageVector.Builder(
            name = "ic_arrow_right_left_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.5166 11.3311C7.90714 10.941 8.54026 10.9408 8.93066 11.3311C9.32098 11.7216 9.32082 12.3547 8.93066 12.7452L7.15723 14.5177H19.2432C19.7951 14.518 20.2432 14.9656 20.2432 15.5177C20.243 16.0696 19.795 16.5173 19.2432 16.5177H7.15625L8.93066 18.2921C9.32103 18.6824 9.32072 19.3156 8.93066 19.7061C8.54014 20.0967 7.90713 20.0967 7.5166 19.7061L4.03516 16.2257C3.84773 16.0381 3.74216 15.7828 3.74219 15.5177C3.74244 15.2527 3.84777 14.998 4.03516 14.8106L7.5166 11.3311Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.0527 4.29305C15.4432 3.9028 16.0763 3.9028 16.4668 4.29305L19.9482 7.77352C20.1357 7.96098 20.2411 8.21643 20.2412 8.48153C20.2411 8.74652 20.1356 9.00116 19.9482 9.18856L16.4668 12.6681C16.0762 13.0585 15.4432 13.0586 15.0527 12.6681C14.6626 12.2775 14.6624 11.6444 15.0527 11.254L16.8262 9.48153H4.74219C4.18998 9.48144 3.74219 9.03376 3.74219 8.48153C3.74241 7.92949 4.19012 7.48162 4.74219 7.48153H16.8271L15.0527 5.70712C14.6625 5.31666 14.6625 4.68353 15.0527 4.29305Z"),
            )
        }.build()
        return _ic_arrow_right_left_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRightLeft24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_right_left_24,
        contentDescription = null,
    )
}