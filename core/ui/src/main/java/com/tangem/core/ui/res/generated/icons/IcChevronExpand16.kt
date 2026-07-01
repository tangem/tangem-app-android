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

private var _ic_chevron_expand_16: ImageVector? = null

val Icons.ic_chevron_expand_16: ImageVector
    get() {
        if (_ic_chevron_expand_16 != null) return _ic_chevron_expand_16!!
        _ic_chevron_expand_16 = ImageVector.Builder(
            name = "ic_chevron_expand_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.65039 7.375C3.99521 7.37526 4.27518 7.65516 4.27539 8V11.7256H8C8.34495 11.7258 8.625 12.0056 8.625 12.3506C8.6247 12.6953 8.34477 12.9753 8 12.9756H4.15039C3.52926 12.9756 3.02472 12.4716 3.02441 11.8506V8C3.02463 7.655 3.30534 7.375 3.65039 7.375ZM11.8506 3.02539C12.4716 3.02576 12.9756 3.5293 12.9756 4.15039V8C12.9754 8.34478 12.6953 8.62463 12.3506 8.625C12.0055 8.625 11.7258 8.34501 11.7256 8V4.27539H8C7.65512 4.27518 7.3752 3.99527 7.375 3.65039C7.375 3.30534 7.655 3.0256 8 3.02539H11.8506Z"),
            )
        }.build()
        return _ic_chevron_expand_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronExpand16Preview() {
    Icon(
        imageVector = Icons.ic_chevron_expand_16,
        contentDescription = null,
    )
}