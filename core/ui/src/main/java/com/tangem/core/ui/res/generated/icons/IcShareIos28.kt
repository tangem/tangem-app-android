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

private var _ic_share_ios_28: ImageVector? = null

val Icons.ic_share_ios_28: ImageVector
    get() {
        if (_ic_share_ios_28 != null) return _ic_share_ios_28!!
        _ic_share_ios_28 = ImageVector.Builder(
            name = "ic_share_ios_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.75 9.15625C10.4403 9.15635 11 9.71596 11 10.4062C10.9999 11.0964 10.4402 11.6561 9.75 11.6562H8.75C8.05976 11.6564 7.5 12.216 7.5 12.9062V21.75C7.50014 22.4402 8.05985 22.9999 8.75 23H19.25C19.9402 22.9999 20.4999 22.4402 20.5 21.75V12.9062C20.5 12.216 19.9403 11.6564 19.25 11.6562H18.25C17.5598 11.6561 17.0001 11.0964 17 10.4062C17 9.71598 17.5598 9.15638 18.25 9.15625H19.25C21.321 9.15635 23 10.8352 23 12.9062V21.75C22.9999 23.8209 21.3209 25.4999 19.25 25.5H8.75C6.67913 25.4999 5.00014 23.8209 5 21.75V12.9062C5 10.8353 6.67905 9.15638 8.75 9.15625H9.75Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2.00098C14.3314 2.00103 14.6494 2.13289 14.8838 2.36719L17.8906 5.37402C18.3784 5.86217 18.3786 6.65355 17.8906 7.1416C17.4026 7.62945 16.6112 7.62935 16.123 7.1416L15.25 6.26855V15.8096C15.2499 16.4998 14.6902 17.0595 14 17.0596C13.3098 17.0594 12.7501 16.4998 12.75 15.8096V6.26855L11.877 7.1416C11.3888 7.62941 10.5974 7.62951 10.1094 7.1416C9.62144 6.65353 9.62155 5.86216 10.1094 5.37402L13.1162 2.36719L13.208 2.28418C13.4303 2.10208 13.7101 2.00104 14 2.00098Z"),
            )
        }.build()
        return _ic_share_ios_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShareIos28Preview() {
    Icon(
        imageVector = Icons.ic_share_ios_28,
        contentDescription = null,
    )
}