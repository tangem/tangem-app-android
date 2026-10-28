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

private var _il_account_user: ImageVector? = null

val Icons.il_account_user: ImageVector
    get() {
        if (_il_account_user != null) return _il_account_user!!
        _il_account_user = ImageVector.Builder(
            name = "il_account_user",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.27539 4.42407C10.3724 2.45075 13.5915 2.45078 15.6885 4.42407C15.6912 4.42667 15.6936 4.4302 15.6963 4.43286C15.7005 4.43692 15.7058 4.44046 15.71 4.44458C17.8001 6.53465 17.8319 9.90444 15.6885 11.9221C13.6328 13.8567 10.2751 14.0494 8.23242 11.8791C6.29804 9.82354 6.10513 6.46655 8.27539 4.42407Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.5684 14.6252C17.6529 14.6253 19.9987 17.1019 19.999 20.0559C19.999 20.6082 19.5513 21.0559 18.999 21.0559H5.00098C4.4488 21.0558 4.00098 20.6081 4.00098 20.0559C4.00127 16.9716 6.47857 14.6254 9.43262 14.6252H14.5684Z"),
            )
        }.build()
        return _il_account_user!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountUserPreview() {
    Icon(
        imageVector = Icons.il_account_user,
        contentDescription = null,
    )
}