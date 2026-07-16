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

private var _ic_shield_checkmark_28: ImageVector? = null

val Icons.ic_shield_checkmark_28: ImageVector
    get() {
        if (_ic_shield_checkmark_28 != null) return _ic_shield_checkmark_28!!
        _ic_shield_checkmark_28 = ImageVector.Builder(
            name = "ic_shield_checkmark_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.1162 11.1165C17.6044 10.6283 18.3956 10.6283 18.8838 11.1165C19.3718 11.6046 19.3719 12.3959 18.8838 12.884L13.8838 17.884C13.3957 18.372 12.6043 18.372 12.1162 17.884L9.11621 14.884C8.6281 14.3959 8.62817 13.6046 9.11621 13.1165C9.60437 12.6283 10.3956 12.6283 10.8838 13.1165L13 15.2327L17.1162 11.1165Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.8682 3.33717C13.3459 2.81595 14.9686 2.85286 16.4258 3.4485L16.4248 3.44948L21.9805 5.71803C23.2841 6.25126 24.1385 7.51916 24.1387 8.92897V12.886C24.1385 18.5242 19.9249 23.7289 14.2734 24.9973C14.0934 25.0376 13.9065 25.0377 13.7266 24.9973C8.07491 23.729 3.86147 18.5243 3.86133 12.886V8.92897C3.86148 7.52042 4.71425 6.25057 6.01953 5.71803L11.5752 3.4485L11.8682 3.33717ZM15.4795 5.76295C14.5316 5.3755 13.4684 5.37559 12.5205 5.76295L6.96484 8.03249C6.59998 8.18132 6.36148 8.53668 6.36133 8.92897V12.886C6.36147 17.2329 9.59786 21.3653 14 22.4895C18.4022 21.3653 21.6385 17.2329 21.6387 12.886V8.92897C21.6385 8.58648 21.4573 8.27169 21.166 8.09791L21.0352 8.03249L15.4795 5.76295Z"),
            )
        }.build()
        return _ic_shield_checkmark_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShieldCheckmark28Preview() {
    Icon(
        imageVector = Icons.ic_shield_checkmark_28,
        contentDescription = null,
    )
}