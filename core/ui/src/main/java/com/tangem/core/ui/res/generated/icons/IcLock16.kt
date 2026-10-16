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

private var _ic_lock_16: ImageVector? = null

val Icons.ic_lock_16: ImageVector
    get() {
        if (_ic_lock_16 != null) return _ic_lock_16!!
        _ic_lock_16 = ImageVector.Builder(
            name = "ic_lock_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 2.99976C9.41495 2.99983 10.5537 4.15503 10.5537 5.56909V5.92358C11.3685 5.9964 12 6.68486 12 7.5144V11.4031C11.9999 12.2803 11.2936 12.9995 10.4111 12.9998H5.58887C4.70637 12.9995 4.00012 12.2803 4 11.4031V7.5144C4 6.68486 4.63146 5.9964 5.44629 5.92358V5.56909C5.44635 4.15498 6.58498 2.99976 8 2.99976ZM5.58887 7.16675C5.40628 7.16698 5.25 7.31774 5.25 7.5144V11.4031C5.25011 11.5996 5.40634 11.7495 5.58887 11.7498H10.4111C10.5937 11.7495 10.7499 11.5996 10.75 11.4031V7.5144C10.75 7.31774 10.5937 7.16698 10.4111 7.16675H5.58887ZM8 4.24976C7.28498 4.24976 6.69635 4.83566 6.69629 5.56909V5.91675H9.30371V5.56909C9.30365 4.83571 8.71496 4.24983 8 4.24976Z"),
            )
        }.build()
        return _ic_lock_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLock16Preview() {
    Icon(
        imageVector = Icons.ic_lock_16,
        contentDescription = null,
    )
}