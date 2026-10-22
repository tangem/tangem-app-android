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

private var _ic_at_sign_16: ImageVector? = null

val Icons.ic_at_sign_16: ImageVector
    get() {
        if (_ic_at_sign_16 != null) return _ic_at_sign_16!!
        _ic_at_sign_16 = ImageVector.Builder(
            name = "ic_at_sign_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.76367 8C9.76367 7.02591 8.97409 6.23633 8 6.23633C7.02591 6.23633 6.23633 7.02591 6.23633 8C6.23633 8.97409 7.02591 9.76367 8 9.76367C8.97409 9.76367 9.76367 8.97409 9.76367 8ZM11.0137 8.89551C11.0137 9.37506 11.4023 9.76361 11.8818 9.76367C12.3614 9.76367 12.75 9.37509 12.75 8.89551V8C12.75 5.37639 10.6236 3.25 8 3.25C5.37639 3.25 3.25 5.37639 3.25 8C3.25 10.6236 5.37639 12.75 8 12.75C8.60666 12.75 9.22262 12.6351 9.81738 12.3887C10.1354 12.2569 10.4324 12.0908 10.709 11.8984C10.9923 11.7014 11.382 11.7704 11.5791 12.0537C11.7761 12.3369 11.7058 12.7266 11.4229 12.9238C11.0795 13.1626 10.7041 13.3738 10.2959 13.543C9.5446 13.8543 8.76567 14 8 14C4.68603 14 2 11.314 2 8C2 4.68603 4.68603 2 8 2C11.314 2 14 4.68603 14 8V8.89551C14 10.0654 13.0518 11.0137 11.8818 11.0137C11.1642 11.0136 10.5315 10.6562 10.1484 10.1104C9.60159 10.667 8.84191 11.0137 8 11.0137C6.33556 11.0137 4.98633 9.66444 4.98633 8C4.98633 6.33556 6.33556 4.98633 8 4.98633C9.66444 4.98633 11.0137 6.33556 11.0137 8V8.89551Z"),
            )
        }.build()
        return _ic_at_sign_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtSign16Preview() {
    Icon(
        imageVector = Icons.ic_at_sign_16,
        contentDescription = null,
    )
}