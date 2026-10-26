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

private var _il_account_heart: ImageVector? = null

val Icons.il_account_heart: ImageVector
    get() {
        if (_il_account_heart != null) return _il_account_heart!!
        _il_account_heart = ImageVector.Builder(
            name = "il_account_heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.7002 3C19.5684 3.00012 21.9999 6.58601 22 9.75977C22 11.4326 21.3441 12.9707 20.4434 14.2969C19.5418 15.6243 18.3575 16.7964 17.2002 17.7598C16.0389 18.7264 14.8765 19.5063 13.9951 20.0449C13.5539 20.3145 13.1785 20.5261 12.9033 20.6729C12.7665 20.7458 12.6494 20.8062 12.5586 20.8496C12.5147 20.8706 12.4679 20.8918 12.4238 20.9102C12.403 20.9188 12.3699 20.9316 12.332 20.9443C12.3134 20.9506 12.2814 20.9611 12.2422 20.9707C12.2183 20.9765 12.1232 21 12 21C11.8768 21 11.7817 20.9765 11.7578 20.9707C11.7186 20.9611 11.6866 20.9506 11.668 20.9443C11.6301 20.9316 11.597 20.9188 11.5762 20.9102C11.5321 20.8918 11.4853 20.8706 11.4414 20.8496C11.3506 20.8062 11.2335 20.7458 11.0967 20.6729C10.8215 20.5261 10.4461 20.3145 10.0049 20.0449C9.12346 19.5063 7.96108 18.7264 6.7998 17.7598C5.64249 16.7964 4.45822 15.6243 3.55664 14.2969C2.65593 12.9707 2 11.4326 2 9.75977C2.00012 6.58601 4.43162 3.00012 8.2998 3C9.93065 3 11.1475 3.61256 12 4.31543C12.8525 3.61256 14.0693 3 15.7002 3Z"),
            )
        }.build()
        return _il_account_heart!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountHeartPreview() {
    Icon(
        imageVector = Icons.il_account_heart,
        contentDescription = null,
    )
}