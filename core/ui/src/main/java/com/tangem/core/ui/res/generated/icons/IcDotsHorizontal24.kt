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

private var _ic_dots_horizontal_24: ImageVector? = null

val Icons.ic_dots_horizontal_24: ImageVector
    get() {
        if (_ic_dots_horizontal_24 != null) return _ic_dots_horizontal_24!!
        _ic_dots_horizontal_24 = ImageVector.Builder(
            name = "ic_dots_horizontal_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.09953 10.5059C6.87727 10.5599 7.49688 11.2045 7.49699 12C7.49686 12.8273 6.82428 13.5 5.99699 13.5C5.17312 13.4997 4.50325 12.8325 4.49797 12.0098L4.49699 12.0107C4.48844 11.2094 5.11151 10.5611 5.88761 10.5059C5.92247 10.5022 5.95823 10.5 5.99406 10.5C6.02951 10.5 6.06503 10.5023 6.09953 10.5059Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1025 10.5059C12.8801 10.56 13.4998 11.2045 13.4999 12C13.4998 12.8272 12.8271 13.4999 11.9999 13.5C11.1759 13.4999 10.5062 12.8326 10.5009 12.0098L10.4999 12.0107C10.4914 11.2093 11.1143 10.5609 11.8905 10.5059C11.9255 10.5022 11.9611 10.5 11.997 10.5C12.0325 10.5 12.0679 10.5022 12.1025 10.5059Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.1044 10.5059C18.8821 10.5599 19.5018 11.2045 19.5019 12C19.5017 12.8273 18.8292 13.5 18.0019 13.5C17.178 13.4997 16.5081 12.8325 16.5028 12.0098L16.5019 12.0107C16.4933 11.2094 17.1164 10.5611 17.8925 10.5059C17.9274 10.5022 17.9631 10.5 17.9989 10.5C18.0344 10.5 18.0699 10.5023 18.1044 10.5059Z"),
            )
        }.build()
        return _ic_dots_horizontal_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDotsHorizontal24Preview() {
    Icon(
        imageVector = Icons.ic_dots_horizontal_24,
        contentDescription = null,
    )
}