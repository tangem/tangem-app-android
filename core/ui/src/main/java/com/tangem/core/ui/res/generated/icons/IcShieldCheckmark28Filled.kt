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

private var _ic_shield_checkmark_28_filled: ImageVector? = null

val Icons.ic_shield_checkmark_28_filled: ImageVector
    get() {
        if (_ic_shield_checkmark_28_filled != null) return _ic_shield_checkmark_28_filled!!
        _ic_shield_checkmark_28_filled = ImageVector.Builder(
            name = "ic_shield_checkmark_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.8682 3.33717C13.3459 2.81595 14.9686 2.85286 16.4258 3.4485L16.4248 3.44948L21.9805 5.71803C23.2841 6.25126 24.1385 7.51916 24.1387 8.92897V12.886C24.1385 18.5242 19.9249 23.7289 14.2734 24.9973C14.0934 25.0376 13.9065 25.0377 13.7266 24.9973C8.07491 23.729 3.86147 18.5243 3.86133 12.886V8.92897C3.86148 7.52042 4.71425 6.25057 6.01953 5.71803L11.5752 3.4485L11.8682 3.33717ZM18.8838 11.1165C18.3956 10.6283 17.6044 10.6283 17.1162 11.1165L13 15.2327L10.8838 13.1165C10.3956 12.6283 9.60437 12.6283 9.11621 13.1165C8.62817 13.6046 8.6281 14.3959 9.11621 14.884L12.1162 17.884C12.6043 18.372 13.3957 18.372 13.8838 17.884L18.8838 12.884C19.3719 12.3959 19.3718 11.6046 18.8838 11.1165Z"),
            )
        }.build()
        return _ic_shield_checkmark_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShieldCheckmark28FilledPreview() {
    Icon(
        imageVector = Icons.ic_shield_checkmark_28_filled,
        contentDescription = null,
    )
}