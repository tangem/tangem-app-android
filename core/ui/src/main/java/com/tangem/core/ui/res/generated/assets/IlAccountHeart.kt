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
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M27.7002 15C31.5684 15.0001 33.9999 18.586 34 21.7598C34 23.4326 33.3441 24.9707 32.4434 26.2969C31.5418 27.6243 30.3575 28.7964 29.2002 29.7598C28.0389 30.7264 26.8765 31.5063 25.9951 32.0449C25.5539 32.3145 25.1785 32.5261 24.9033 32.6729C24.7665 32.7458 24.6494 32.8062 24.5586 32.8496C24.5147 32.8706 24.4679 32.8918 24.4238 32.9102C24.403 32.9188 24.3699 32.9316 24.332 32.9443C24.3134 32.9506 24.2814 32.9611 24.2422 32.9707C24.2183 32.9765 24.1232 33 24 33C23.8768 33 23.7817 32.9765 23.7578 32.9707C23.7186 32.9611 23.6866 32.9506 23.668 32.9443C23.6301 32.9316 23.597 32.9188 23.5762 32.9102C23.5321 32.8918 23.4853 32.8706 23.4414 32.8496C23.3506 32.8062 23.2335 32.7458 23.0967 32.6729C22.8215 32.5261 22.4461 32.3145 22.0049 32.0449C21.1235 31.5063 19.9611 30.7264 18.7998 29.7598C17.6425 28.7964 16.4582 27.6243 15.5566 26.2969C14.6559 24.9707 14 23.4326 14 21.7598C14.0001 18.586 16.4316 15.0001 20.2998 15C21.9307 15 23.1475 15.6126 24 16.3154C24.8525 15.6126 26.0693 15 27.7002 15Z"),
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