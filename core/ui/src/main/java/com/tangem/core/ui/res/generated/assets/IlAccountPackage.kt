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

private var _il_account_package: ImageVector? = null

val Icons.il_account_package: ImageVector
    get() {
        if (_il_account_package != null) return _il_account_package!!
        _il_account_package = ImageVector.Builder(
            name = "il_account_package",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.41699 4.92805L15.082 9.35285L16.582 8.48762L8.91699 4.06281L7.41699 4.92805Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.6074 1.35578C11.4696 0.858686 12.5314 0.858552 13.3936 1.35578L20.5225 5.47102C21.3853 5.96871 21.9149 6.88958 21.915 7.8841V16.1155C21.915 17.1114 21.3842 18.032 20.5215 18.5296L20.5205 18.5286L13.3936 22.6439C12.5315 23.1411 11.4696 23.1418 10.6074 22.6448L10.6064 22.6439L3.47852 18.5286C2.61674 18.0301 2.08496 17.1105 2.08496 16.1146V7.8841C2.08507 6.88835 2.61586 5.9686 3.47852 5.47102L5.94721 4.04594L7.41699 3.19749L10.6074 1.35578ZM11.999 11.1341L15.082 9.35285L16.582 8.48762L19.665 6.70735L12.6436 2.65461C12.2453 2.42511 11.7547 2.42506 11.3564 2.65461L8.91699 4.06281L7.41699 4.92805L5.87549 5.81819L4.33398 6.70832L11.999 11.1341ZM6.26562 14.6497C5.90717 14.4423 5.44775 14.5647 5.24023 14.9232C5.03282 15.2816 5.15533 15.741 5.51367 15.9486L8.60938 17.7405C8.9678 17.9481 9.42719 17.8255 9.63477 17.4671C9.84211 17.1087 9.71958 16.6503 9.36133 16.4427L6.26562 14.6497Z"),
            )
        }.build()
        return _il_account_package!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountPackagePreview() {
    Icon(
        imageVector = Icons.il_account_package,
        contentDescription = null,
    )
}