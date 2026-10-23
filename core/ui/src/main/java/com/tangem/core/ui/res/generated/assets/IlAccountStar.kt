@file:Suppress("all")

package com.tangem.core.ui.res.generated.assets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.generated.icons.Icons

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _il_account_star: ImageVector? = null

val Icons.il_account_star: ImageVector
    get() {
        if (_il_account_star != null) return _il_account_star!!
        _il_account_star = ImageVector.Builder(
            name = "il_account_star",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.0004 2.24219C12.8544 2.24234 13.6347 2.72587 14.0151 3.49023L15.974 7.39941L20.3149 8.02051C21.1695 8.14041 21.8816 8.73895 22.1459 9.56055C22.41 10.3815 22.1805 11.2803 21.5571 11.876L21.558 11.877L18.4389 14.8652L19.1801 19.1123C19.329 19.9561 18.9853 20.8115 18.2944 21.3184C17.6035 21.8249 16.6851 21.8943 15.9252 21.499L11.9994 19.4717L8.07561 21.499L8.07463 21.498C7.31492 21.893 6.39721 21.8237 5.70647 21.3174C5.01466 20.8101 4.67073 19.9531 4.82072 19.1084L5.56096 14.8652L2.44279 11.877L2.44377 11.876C1.82008 11.2803 1.59078 10.3817 1.8549 9.56055C2.1027 8.79046 2.74266 8.21638 3.5258 8.04883L3.68498 8.02051L8.02385 7.39941L9.98479 3.49023C10.3652 2.72571 11.1463 2.24219 12.0004 2.24219Z"),
            )
        }.build()
        return _il_account_star!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountStarPreview() {
    Icon(
        imageVector = Icons.il_account_star,
        contentDescription = null,
    )
}