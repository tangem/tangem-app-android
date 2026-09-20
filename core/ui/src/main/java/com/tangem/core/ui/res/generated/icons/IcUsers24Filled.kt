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

private var _ic_users_24_filled: ImageVector? = null

val Icons.ic_users_24_filled: ImageVector
    get() {
        if (_ic_users_24_filled != null) return _ic_users_24_filled!!
        _ic_users_24_filled = ImageVector.Builder(
            name = "ic_users_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.37598 4.53271C10.6202 4.53271 12.5039 6.29516 12.5039 8.54639C12.5037 10.7974 10.6201 12.5601 8.37598 12.5601C6.13216 12.5597 4.24924 10.7972 4.24902 8.54639C4.24902 6.29537 6.13203 4.53304 8.37598 4.53271Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.4424 6.2417C18.1965 6.24191 19.6826 7.62192 19.6826 9.40088C19.6824 11.1796 18.1964 12.5589 16.4424 12.5591C14.6882 12.5591 13.2024 11.1798 13.2021 9.40088C13.2021 7.62179 14.6881 6.2417 16.4424 6.2417Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.00098 19.4673C2.44888 19.4671 2.00098 19.0194 2.00098 18.4673V17.5894C2.00102 15.103 4.0821 13.1501 6.57031 13.1499H10.1846C11.1562 13.1499 12.0658 13.4477 12.8135 13.9587C13.5613 13.4477 14.4708 13.1499 15.4424 13.1499H16.4424H17.4297C19.918 13.15 21.999 15.1029 21.999 17.5894V18.4673C21.999 19.0194 21.5511 19.4671 20.999 19.4673H3.00098Z"),
            )
        }.build()
        return _ic_users_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUsers24FilledPreview() {
    Icon(
        imageVector = Icons.ic_users_24_filled,
        contentDescription = null,
    )
}