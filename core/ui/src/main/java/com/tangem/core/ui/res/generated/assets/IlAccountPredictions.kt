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
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.33333 0C11.3834 0 14.6667 3.28325 14.6667 7.33333C14.6667 11.3834 11.3834 14.6667 7.33333 14.6667C3.28325 14.6667 0 11.3834 0 7.33333C0 3.28325 3.28325 0 7.33333 0ZM10.5378 5.26888C10.1193 4.85051 9.4412 4.85047 9.02279 5.26888L6.6849 7.60677L6.01432 6.93555C5.59589 6.51712 4.91713 6.51712 4.4987 6.93555C4.08047 7.35393 4.08048 8.03213 4.4987 8.45052L5.92773 9.87891C6.34616 10.2973 7.02428 10.2973 7.44271 9.87891L10.5378 6.78385C10.9562 6.36543 10.9562 5.68731 10.5378 5.26888Z"),
            )
            addPath(
                fill = SolidColor(Color.White),
                fillAlpha = 0.8f,
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.6669 9.3335C20.7169 9.33361 24.0002 12.6168 24.0002 16.6668C24.0001 20.7167 20.7168 24 16.6669 24.0002C12.6169 24.0002 9.33367 20.7168 9.33356 16.6668C9.33356 16.3573 9.35427 16.0522 9.3915 15.7528C12.5246 14.9896 14.9903 12.5245 15.7535 9.39144C16.0527 9.35426 16.3576 9.3335 16.6669 9.3335ZM15.952 14.3348C15.5059 13.8884 14.7826 13.8887 14.3362 14.3348C13.8899 14.7809 13.8889 15.5042 14.3349 15.9507L15.0504 16.6649L14.3349 17.381C13.8884 17.8272 13.8887 18.5504 14.3349 18.9969C14.7809 19.4432 15.5042 19.4446 15.9507 18.9989L16.6649 18.2827L17.3811 18.9989C17.8272 19.445 18.5505 19.4447 18.997 18.9989C19.4433 18.5528 19.4448 17.8288 18.9989 17.3823L18.2828 16.6668L18.9989 15.952C19.445 15.5058 19.4448 14.7825 18.9989 14.3361C18.5528 13.8897 17.8289 13.8888 17.3824 14.3348L16.6669 15.0503L15.952 14.3348Z"),
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