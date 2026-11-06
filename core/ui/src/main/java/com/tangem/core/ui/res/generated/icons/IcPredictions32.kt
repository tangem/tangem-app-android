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

private var _ic_predictions_32: ImageVector? = null

val Icons.ic_predictions_32: ImageVector
    get() {
        if (_ic_predictions_32 != null) return _ic_predictions_32!!
        _ic_predictions_32 = ImageVector.Builder(
            name = "ic_predictions_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.2729 17.9379C21.8586 17.3523 22.8082 17.3523 23.3939 17.9379C23.9795 18.5237 23.9796 19.4733 23.3939 20.059L22.7875 20.6654L23.3949 21.2729C23.9799 21.8587 23.9803 22.8084 23.3949 23.3939C22.8092 23.9794 21.8586 23.9793 21.2729 23.3939L20.6654 22.7865L20.06 23.393C19.4742 23.9784 18.5246 23.9784 17.9389 23.393C17.3533 22.8073 17.3534 21.8576 17.9389 21.2719L18.5443 20.6654L17.9389 20.06C17.3534 19.4744 17.3537 18.5247 17.9389 17.9389C18.5247 17.3531 19.4751 17.3531 20.0609 17.9389L20.6664 18.5443L21.2729 17.9379Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.6664 11.8324C25.5451 11.8325 29.5004 15.7876 29.5004 20.6664C29.5003 25.5451 25.5451 29.5003 20.6664 29.5004C15.7876 29.5004 11.8325 25.5451 11.8324 20.6664C11.8324 15.7876 15.7876 11.8324 20.6664 11.8324ZM20.6664 14.8324C17.4445 14.8324 14.8324 17.4445 14.8324 20.6664C14.8325 23.8883 17.4445 26.5004 20.6664 26.5004C23.8882 26.5003 26.5003 23.8882 26.5004 20.6664C26.5004 17.4445 23.8883 14.8325 20.6664 14.8324Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.5248 2.5375C14.7637 2.14741 18.6779 4.83668 19.8354 8.93301C20.0605 9.73018 19.5973 10.5593 18.8002 10.7846C18.003 11.0096 17.1738 10.5456 16.9486 9.74844C16.1842 7.04347 13.5993 5.26736 10.8002 5.5248C8.00099 5.78257 5.78257 8.00099 5.5248 10.8002C5.26736 13.5993 7.04347 16.1842 9.74844 16.9486C10.5456 17.1738 11.0096 18.003 10.7846 18.8002C10.5593 19.5973 9.73018 20.0605 8.93301 19.8354C4.83668 18.6779 2.14741 14.7637 2.5375 10.5248C2.92782 6.28611 6.28611 2.92782 10.5248 2.5375Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7162 9.04922C13.3019 8.46352 14.2514 8.46367 14.8373 9.04922C15.423 9.63478 15.4236 10.5844 14.8383 11.1703L11.7846 14.226C11.199 14.8118 10.2485 14.8123 9.6625 14.227L7.82754 12.393C7.24185 11.8075 7.24132 10.8578 7.82656 10.2719C8.41211 9.68607 9.36267 9.68545 9.94863 10.2709L10.7221 11.0434L12.7162 9.04922Z"),
            )
        }.build()
        return _ic_predictions_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPredictions32Preview() {
    Icon(
        imageVector = Icons.ic_predictions_32,
        contentDescription = null,
    )
}