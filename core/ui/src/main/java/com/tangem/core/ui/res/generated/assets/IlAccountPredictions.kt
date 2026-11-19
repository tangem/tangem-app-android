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

private var _il_account_predictions: ImageVector? = null

val Icons.il_account_predictions: ImageVector
    get() {
        if (_il_account_predictions != null) return _il_account_predictions!!
        _il_account_predictions = ImageVector.Builder(
            name = "il_account_predictions",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.3333 12C23.3834 12 26.6667 15.2832 26.6667 19.3333C26.6667 23.3834 23.3834 26.6667 19.3333 26.6667C15.2832 26.6667 12 23.3834 12 19.3333C12 15.2832 15.2832 12 19.3333 12ZM22.5378 17.2689C22.1193 16.8505 21.4412 16.8505 21.0228 17.2689L18.6849 19.6068L18.0143 18.9355C17.5959 18.5171 16.9171 18.5171 16.4987 18.9355C16.0805 19.3539 16.0805 20.0321 16.4987 20.4505L17.9277 21.8789C18.3462 22.2973 19.0243 22.2973 19.4427 21.8789L22.5378 18.7839C22.9562 18.3654 22.9562 17.6873 22.5378 17.2689Z"),
            )
            addPath(
                fill = SolidColor(Color.White),
                fillAlpha = 0.8f,
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M28.6669 21.3335C32.7169 21.3336 36.0002 24.6168 36.0002 28.6668C36.0001 32.7167 32.7168 36 28.6669 36.0002C24.6169 36.0002 21.3337 32.7168 21.3336 28.6668C21.3336 28.3573 21.3543 28.0522 21.3915 27.7528C24.5246 26.9896 26.9903 24.5245 27.7535 21.3914C28.0527 21.3543 28.3576 21.3335 28.6669 21.3335ZM27.952 26.3348C27.5059 25.8884 26.7826 25.8887 26.3362 26.3348C25.8899 26.7809 25.8889 27.5042 26.3349 27.9507L27.0504 28.6649L26.3349 29.381C25.8884 29.8272 25.8887 30.5504 26.3349 30.9969C26.7809 31.4432 27.5042 31.4446 27.9507 30.9989L28.6649 30.2827L29.3811 30.9989C29.8272 31.445 30.5505 31.4447 30.997 30.9989C31.4433 30.5528 31.4448 29.8288 30.9989 29.3823L30.2828 28.6668L30.9989 27.952C31.445 27.5058 31.4448 26.7825 30.9989 26.3361C30.5528 25.8897 29.8289 25.8888 29.3824 26.3348L28.6669 27.0503L27.952 26.3348Z"),
            )
        }.build()
        return _il_account_predictions!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountPredictionsPreview() {
    Icon(
        imageVector = Icons.il_account_predictions,
        contentDescription = null,
    )
}