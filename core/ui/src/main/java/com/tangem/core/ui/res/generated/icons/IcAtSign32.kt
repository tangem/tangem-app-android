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

private var _ic_at_sign_32: ImageVector? = null

val Icons.ic_at_sign_32: ImageVector
    get() {
        if (_ic_at_sign_32 != null) return _ic_at_sign_32!!
        _ic_at_sign_32 = ImageVector.Builder(
            name = "ic_at_sign_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.833 16C19.833 13.8831 18.1169 12.167 16 12.167C13.8831 12.167 12.167 13.8831 12.167 16C12.167 18.1169 13.8831 19.833 16 19.833C18.1169 19.833 19.833 18.1169 19.833 16ZM22.833 18C22.833 19.0129 23.6541 19.833 24.667 19.833C25.6797 19.8328 26.5 19.0128 26.5 18V16C26.5 10.2004 21.7996 5.5 16 5.5C10.2004 5.5 5.5 10.2004 5.5 16C5.5 21.7996 10.2004 26.5 16 26.5C17.3412 26.5 18.7028 26.246 20.0176 25.7012C20.7201 25.41 21.3753 25.0419 21.9873 24.6162C22.6673 24.1433 23.6021 24.3114 24.0752 24.9912C24.5482 25.6713 24.3803 26.6061 23.7002 27.0791C22.9282 27.6161 22.0848 28.0918 21.166 28.4727C19.4756 29.1731 17.7227 29.5 16 29.5C8.54357 29.5 2.5 23.4564 2.5 16C2.5 8.54357 8.54357 2.5 16 2.5C23.4564 2.5 29.5 8.54357 29.5 16V18C29.5 20.6697 27.3366 22.8328 24.667 22.833C23.0756 22.833 21.6639 22.0644 20.7832 20.8779C19.5505 22.0868 17.8628 22.833 16 22.833C12.2262 22.833 9.16699 19.7738 9.16699 16C9.16699 12.2262 12.2262 9.16699 16 9.16699C19.7738 9.16699 22.833 12.2262 22.833 16V18Z"),
            )
        }.build()
        return _ic_at_sign_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtSign32Preview() {
    Icon(
        imageVector = Icons.ic_at_sign_32,
        contentDescription = null,
    )
}