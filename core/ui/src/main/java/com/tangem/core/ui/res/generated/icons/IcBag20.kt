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

private var _ic_bag_20: ImageVector? = null

val Icons.ic_bag_20: ImageVector
    get() {
        if (_ic_bag_20 != null) return _ic_bag_20!!
        _ic_bag_20 = ImageVector.Builder(
            name = "ic_bag_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.417 7.63867C12.831 7.63894 13.167 7.97462 13.167 8.38867V9.19434C13.1669 10.8882 11.8367 12.2712 10.1641 12.3564L10.001 12.3613L9.83789 12.3564C8.16468 12.2717 6.83405 10.8886 6.83398 9.19434V8.38867C6.83398 7.97446 7.16977 7.63867 7.58398 7.63867C7.99797 7.63894 8.33398 7.97462 8.33398 8.38867V9.19434C8.33405 10.1147 9.08052 10.8613 10.001 10.8613C10.9211 10.8609 11.6669 10.1145 11.667 9.19434V8.38867C11.667 7.97458 12.0029 7.63886 12.417 7.63867Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.834 2C16.1378 2.00026 17.1943 3.05748 17.1943 4.36133V5.00391C17.6876 5.43671 18 6.07062 18 6.77832V15.6387C18 16.9427 16.9427 18 15.6387 18H4.36133C3.05733 18 2 16.9427 2 15.6387V6.77832C2 6.07062 2.31241 5.43671 2.80566 5.00391V4.36133C2.80566 3.05732 3.86299 2 5.16699 2H14.834ZM4.36133 5.91699C3.88573 5.91699 3.5 6.30276 3.5 6.77832V15.6387C3.5 16.1142 3.88573 16.5 4.36133 16.5H15.6387C16.1143 16.5 16.5 16.1142 16.5 15.6387V6.77832C16.5 6.30276 16.1143 5.91699 15.6387 5.91699H4.36133ZM5.16699 3.5C4.6914 3.5 4.30566 3.88577 4.30566 4.36133V4.41895C4.32418 4.41852 4.34271 4.41699 4.36133 4.41699H15.6387C15.6573 4.41699 15.6758 4.41852 15.6943 4.41895V4.36133C15.6943 3.88593 15.3094 3.50026 14.834 3.5H5.16699Z"),
            )
        }.build()
        return _ic_bag_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBag20Preview() {
    Icon(
        imageVector = Icons.ic_bag_20,
        contentDescription = null,
    )
}