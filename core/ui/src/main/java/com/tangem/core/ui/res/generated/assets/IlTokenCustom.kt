@file:Suppress("all")

package com.tangem.core.ui.res.generated.assets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.generated.icons.Icons

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _il_token_custom: ImageVector? = null

val Icons.il_token_custom: ImageVector
    get() {
        if (_il_token_custom != null) return _il_token_custom!!
        _il_token_custom = ImageVector.Builder(
            name = "il_token_custom",
            defaultWidth = 72.dp,
            defaultHeight = 72.dp,
            viewportWidth = 72f,
            viewportHeight = 72f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF989898)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M36 16L37.7316 27.2942C38.2833 30.8928 41.1072 33.7167 44.7058 34.2684L56 36L44.7058 37.7316C41.1072 38.2833 38.2833 41.1072 37.7316 44.7058L36 56L34.2684 44.7058C33.7167 41.1072 30.8928 38.2833 27.2942 37.7316L16 36L27.2942 34.2684C30.8928 33.7167 33.7167 30.8928 34.2684 27.2942L36 16Z"),
            )
        }.build()
        return _il_token_custom!!
    }

@Composable
@Preview(showBackground = true)
private fun IlTokenCustomPreview() {
    Icon(
        imageVector = Icons.il_token_custom,
        contentDescription = null,
    )
}