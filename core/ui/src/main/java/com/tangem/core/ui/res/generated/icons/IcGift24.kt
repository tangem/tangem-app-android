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

private var _ic_gift_24: ImageVector? = null

val Icons.ic_gift_24: ImageVector
    get() {
        if (_ic_gift_24 != null) return _ic_gift_24!!
        _ic_gift_24 = ImageVector.Builder(
            name = "ic_gift_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1074 3.12817C13.2752 1.94706 15.1581 1.95792 16.3135 3.15942C17.3535 4.24134 17.4456 5.91979 16.5947 7.11059H19.7773C20.8662 7.11059 21.7499 7.99441 21.75 9.08325V11.0276C21.7498 12.1069 20.8815 12.9821 19.8057 12.9973V19.7766C19.8057 20.8656 18.922 21.7493 17.833 21.7493H6.16699C5.07804 21.7493 4.19434 20.8656 4.19434 19.7766V12.9973C3.11855 12.9821 2.25017 12.1069 2.25 11.0276V9.08325C2.25013 7.99441 3.13379 7.11059 4.22266 7.11059H7.40527C6.55441 5.91987 6.64574 4.24137 7.68555 3.15942C8.83488 1.96403 10.7039 1.94809 11.873 3.11059C11.8904 3.12626 11.9084 3.14233 11.9248 3.15942C11.9511 3.18682 11.975 3.21619 12 3.24438C12.0253 3.21585 12.0495 3.18616 12.0762 3.15844C12.0863 3.14794 12.097 3.13813 12.1074 3.12817ZM6.19434 12.9993V19.7493H17.8057V12.9993H6.19434ZM4.25 10.9993H19.75V9.11059H4.25V10.9993ZM10.4805 4.54516C10.102 4.15172 9.50536 4.15166 9.12695 4.54516C8.72737 4.96112 8.72729 5.6527 9.12695 6.0686L9.27832 6.21215C9.7669 6.64139 10.4174 6.88292 10.9941 7.00512C10.9994 6.83839 11.0027 6.65872 10.9971 6.4729C10.9847 6.06344 10.9395 5.65125 10.8447 5.29321C10.7485 4.93024 10.618 4.68957 10.4844 4.54907L10.4805 4.54516ZM14.8721 4.54516C14.4936 4.15182 13.8969 4.15163 13.5186 4.54516L13.5137 4.54809C13.3804 4.68867 13.2513 4.93011 13.1553 5.29223C13.0604 5.65025 13.0154 6.06245 13.0029 6.47192C12.9973 6.65794 12.9996 6.83823 13.0049 7.00512C13.6433 6.86999 14.3723 6.58849 14.8721 6.0686C15.2719 5.65263 15.2719 4.96113 14.8721 4.54516Z"),
            )
        }.build()
        return _ic_gift_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGift24Preview() {
    Icon(
        imageVector = Icons.ic_gift_24,
        contentDescription = null,
    )
}