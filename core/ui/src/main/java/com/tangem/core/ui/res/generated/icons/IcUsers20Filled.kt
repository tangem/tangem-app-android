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

private var _ic_users_20_filled: ImageVector? = null

val Icons.ic_users_20_filled: ImageVector
    get() {
        if (_ic_users_20_filled != null) return _ic_users_20_filled!!
        _ic_users_20_filled = ImageVector.Builder(
            name = "ic_users_20_filled",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.08105 4.03027C8.86116 4.0305 10.3495 5.43015 10.3496 7.21191C10.3496 8.9938 8.86124 10.3943 7.08105 10.3945C5.30068 10.3945 3.81153 8.99394 3.81152 7.21191C3.81166 5.43 5.30076 4.03027 7.08105 4.03027Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.5781 5.41016C14.9638 5.4102 16.1328 6.50133 16.1328 7.90234C16.1326 9.30319 14.9637 10.3945 13.5781 10.3945C12.1927 10.3944 11.0236 9.30311 11.0234 7.90234C11.0234 6.50141 12.1926 5.41032 13.5781 5.41016Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M2.75098 15.9697C2.33703 15.9697 2.00134 15.6336 2.00098 15.2197V14.5107C2.00099 12.5389 3.64898 10.9855 5.62598 10.9854H8.53809C9.3247 10.9854 10.0592 11.2313 10.6586 11.6516C11.2579 11.2314 11.9923 10.9854 12.7788 10.9854H13.5781H14.374C16.3509 10.9856 17.999 12.539 17.999 14.5107V15.2197C17.9987 15.6335 17.6629 15.9695 17.249 15.9697H11.4131H9.90381H2.75098Z"),
            )
        }.build()
        return _ic_users_20_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUsers20FilledPreview() {
    Icon(
        imageVector = Icons.ic_users_20_filled,
        contentDescription = null,
    )
}