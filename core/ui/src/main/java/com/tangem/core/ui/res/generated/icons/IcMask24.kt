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

private var _ic_mask_24: ImageVector? = null

val Icons.ic_mask_24: ImageVector
    get() {
        if (_ic_mask_24 != null) return _ic_mask_24!!
        _ic_mask_24 = ImageVector.Builder(
            name = "ic_mask_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.4473 5.11292C19.4495 4.76261 19.2852 4.43154 19.0049 4.22132C18.7247 4.01133 18.361 3.94638 18.0254 4.04651L18.0244 4.04749C14.0758 5.21969 9.87153 5.2197 5.92288 4.04749L5.92191 4.04651C5.58633 3.94637 5.22264 4.01132 4.94241 4.22132C4.66216 4.43154 4.49784 4.7626 4.50003 5.11292V12.527C4.50003 16.6545 7.84611 20.0006 11.9737 20.0006C16.1012 20.0006 19.4473 16.6545 19.4473 12.527V5.11292ZM13.9317 13.9215C14.3316 13.5406 14.9648 13.5558 15.3457 13.9557C15.7266 14.3556 15.7115 14.9889 15.3116 15.3698C13.4425 17.1498 10.5049 17.1498 8.63577 15.3698C8.23585 14.9889 8.22071 14.3556 8.60159 13.9557C8.98248 13.5558 9.61574 13.5406 10.0157 13.9215C11.1122 14.9656 12.8351 14.9656 13.9317 13.9215ZM9.8555 8.34925C10.4076 8.34937 10.8554 8.79711 10.8555 9.34925C10.8555 9.90146 10.4077 10.3491 9.8555 10.3492H7.73734C7.18505 10.3492 6.73734 9.90153 6.73734 9.34925C6.73742 8.79704 7.1851 8.34925 7.73734 8.34925H9.8555ZM16.21 8.34925C16.7622 8.34925 17.2099 8.79704 17.21 9.34925C17.21 9.90153 16.7623 10.3492 16.21 10.3492H14.0918C13.5396 10.3491 13.0918 9.90146 13.0918 9.34925C13.0919 8.79711 13.5397 8.34938 14.0918 8.34925H16.21ZM21.4473 12.527C21.4473 17.7591 17.2058 22.0006 11.9737 22.0006C6.74154 22.0006 2.50003 17.7591 2.50003 12.527V5.11976C2.49567 4.13768 2.95641 3.21115 3.74222 2.62171C4.52906 2.03159 5.54866 1.84988 6.49124 2.1305C10.0685 3.19247 13.8779 3.19147 17.4551 2.12952C18.3979 1.84852 19.418 2.03141 20.2051 2.62171C20.9909 3.21115 21.4516 4.1377 21.4473 5.11976V12.527Z"),
            )
        }.build()
        return _ic_mask_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMask24Preview() {
    Icon(
        imageVector = Icons.ic_mask_24,
        contentDescription = null,
    )
}