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

private var _ic_at_sign_20: ImageVector? = null

val Icons.ic_at_sign_20: ImageVector
    get() {
        if (_ic_at_sign_20 != null) return _ic_at_sign_20!!
        _ic_at_sign_20 = ImageVector.Builder(
            name = "ic_at_sign_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.4727 10C12.4727 8.63474 11.3653 7.52734 10 7.52734C8.63474 7.52734 7.52734 8.63474 7.52734 10C7.52734 11.3653 8.63474 12.4727 10 12.4727C11.3653 12.4727 12.4727 11.3653 12.4727 10ZM13.9727 11.208C13.9727 11.9063 14.5381 12.4727 15.2363 12.4727C15.9345 12.4725 16.5 11.9062 16.5 11.208V10C16.5 6.4098 13.5902 3.5 10 3.5C6.4098 3.5 3.5 6.4098 3.5 10C3.5 13.5902 6.4098 16.5 10 16.5C10.8302 16.5 11.6733 16.3422 12.4873 16.0049C12.9229 15.8243 13.329 15.5969 13.707 15.334C14.047 15.0977 14.5145 15.1815 14.751 15.5215C14.9873 15.8614 14.9033 16.3289 14.5635 16.5654C14.1054 16.884 13.6053 17.1652 13.0615 17.3906C12.0598 17.8057 11.021 18 10 18C5.58137 18 2 14.4186 2 10C2 5.58137 5.58137 2 10 2C14.4186 2 18 5.58137 18 10V11.208C18 12.7346 16.7629 13.9725 15.2363 13.9727C14.2596 13.9727 13.4027 13.4648 12.9111 12.7002C12.1857 13.4819 11.1505 13.9727 10 13.9727C7.80631 13.9727 6.02734 12.1937 6.02734 10C6.02734 7.80631 7.80631 6.02734 10 6.02734C12.1937 6.02734 13.9727 7.80631 13.9727 10V11.208Z"),
            )
        }.build()
        return _ic_at_sign_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtSign20Preview() {
    Icon(
        imageVector = Icons.ic_at_sign_20,
        contentDescription = null,
    )
}