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

private var _ic_card_plus_28: ImageVector? = null

val Icons.ic_card_plus_28: ImageVector
    get() {
        if (_ic_card_plus_28 != null) return _ic_card_plus_28!!
        _ic_card_plus_28 = ImageVector.Builder(
            name = "ic_card_plus_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.167 5.01758C15.8572 5.01775 16.417 5.57733 16.417 6.26758C16.4168 6.95766 15.8571 7.5174 15.167 7.51758H7C5.46193 7.51758 4.5 8.57993 4.5 9.55078V20C4.50021 21.3328 5.63626 22.5 7 22.5H21C22.3627 22.5 23.4998 21.3325 23.5 20V12.834C23.5 12.1436 24.0596 11.584 24.75 11.584C25.4404 11.584 26 12.1436 26 12.834V20C25.9998 22.6902 23.7663 25 21 25H7C4.31847 25 2.14036 22.8322 2.00684 20.251L2 20V9.55078C2 6.89524 4.40619 5.01758 7 5.01758H15.167ZM11.165 17.417C11.8552 17.4173 12.415 17.9768 12.415 18.667C12.4148 19.357 11.855 19.9167 11.165 19.917H8.16699C7.47676 19.917 6.91719 19.3572 6.91699 18.667C6.91699 17.9766 7.47664 17.417 8.16699 17.417H11.165ZM22.75 2.75C23.4404 2.75 24 3.30964 24 4V4.5H24.5C25.1904 4.5 25.75 5.05964 25.75 5.75C25.75 6.44036 25.1904 7 24.5 7H24V7.5C24 8.19036 23.4404 8.75 22.75 8.75C22.0596 8.75 21.5 8.19036 21.5 7.5V7H21C20.3096 7 19.75 6.44036 19.75 5.75C19.75 5.05964 20.3096 4.5 21 4.5H21.5V4C21.5 3.30964 22.0596 2.75 22.75 2.75Z"),
            )
        }.build()
        return _ic_card_plus_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCardPlus28Preview() {
    Icon(
        imageVector = Icons.ic_card_plus_28,
        contentDescription = null,
    )
}