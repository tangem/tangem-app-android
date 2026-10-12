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

private var _ic_users_16_filled: ImageVector? = null

val Icons.ic_users_16_filled: ImageVector
    get() {
        if (_ic_users_16_filled != null) return _ic_users_16_filled!!
        _ic_users_16_filled = ImageVector.Builder(
            name = "ic_users_16_filled",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.8262 4.20166C11.9306 4.20166 12.8261 5.09715 12.8262 6.20166C12.826 7.3061 11.9305 8.20068 10.8262 8.20068C9.72194 8.20049 8.8263 7.30598 8.82617 6.20166C8.82622 5.09727 9.72189 4.20185 10.8262 4.20166Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.7041 3.20654C7.03233 3.20679 8.10926 4.28355 8.10937 5.61181C8.10921 6.94004 7.0323 8.01684 5.7041 8.01709C4.37578 8.01697 3.29899 6.94012 3.29883 5.61181C3.29894 4.28347 4.37575 3.20666 5.7041 3.20654Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M2.62598 12.7934C2.28083 12.7934 2.00103 12.5136 2.00098 12.1684V11.6645C2.00121 10.2044 3.22808 9.06495 4.68554 9.06494H6.77148C7.46514 9.06501 8.10661 9.32318 8.58845 9.75118C9.07029 9.32326 9.7117 9.0651 10.4052 9.06494H11.3096H11.3144C12.7716 9.06529 13.9988 10.2046 13.999 11.6645V12.1684C13.999 12.5134 13.7189 12.7931 13.374 12.7934H8.49701H2.62598Z"),
            )
        }.build()
        return _ic_users_16_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUsers16FilledPreview() {
    Icon(
        imageVector = Icons.ic_users_16_filled,
        contentDescription = null,
    )
}