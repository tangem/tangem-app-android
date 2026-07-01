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

private var _ic_warning_24_filled: ImageVector? = null

val Icons.ic_warning_24_filled: ImageVector
    get() {
        if (_ic_warning_24_filled != null) return _ic_warning_24_filled!!
        _ic_warning_24_filled = ImageVector.Builder(
            name = "ic_warning_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.30546 4.29484C10.4964 2.20901 13.5032 2.20901 14.6941 4.29484L21.5857 16.3554L21.6883 16.5507C22.6724 18.582 21.1993 20.9979 18.8914 20.998H5.10819C2.72588 20.9976 1.23178 18.4239 2.41386 16.3554L9.30546 4.29484ZM11.9998 7.99992C11.4477 8.00013 10.9999 8.44784 10.9998 8.99992V12.4999C11 13.0519 11.4478 13.4997 11.9998 13.4999C12.5519 13.4999 12.9996 13.052 12.9998 12.4999V8.99992C12.9997 8.44774 12.552 7.99997 11.9998 7.99992ZM13.2508 15.9999C13.2508 15.3098 12.6909 14.7502 12.0008 14.7499C11.3104 14.7499 10.7508 15.3096 10.7508 15.9999C10.7509 16.6902 11.3105 17.2499 12.0008 17.2499C12.6908 17.2496 13.2506 16.69 13.2508 15.9999Z"),
            )
        }.build()
        return _ic_warning_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWarning24FilledPreview() {
    Icon(
        imageVector = Icons.ic_warning_24_filled,
        contentDescription = null,
    )
}