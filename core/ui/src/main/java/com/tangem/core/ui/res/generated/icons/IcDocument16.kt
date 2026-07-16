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

private var _ic_document_16: ImageVector? = null

val Icons.ic_document_16: ImageVector
    get() {
        if (_ic_document_16 != null) return _ic_document_16!!
        _ic_document_16 = ImageVector.Builder(
            name = "ic_document_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.82715 10.1348C10.1719 10.1349 10.4518 10.3927 10.4521 10.7109C10.4521 11.0294 10.1722 11.288 9.82715 11.2881H6.17188C5.82673 11.2881 5.54688 11.0295 5.54688 10.7109C5.54727 10.3927 5.82697 10.1348 6.17188 10.1348H9.82715Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.38965 8.17676C7.73439 8.1771 8.01352 8.43566 8.01367 8.75391C8.01317 9.07188 7.73417 9.32974 7.38965 9.33008H6.17188C5.82704 9.33008 5.54738 9.07209 5.54688 8.75391C5.54703 8.43545 5.82682 8.17676 6.17188 8.17676H7.38965Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.67969 2C10.3446 2.00005 10.9736 2.26189 11.4297 2.71289L12.7959 4.06445C13.2504 4.51415 13.5 5.11645 13.5 5.73633V11.6162C13.4996 12.8947 12.441 13.9997 11.0469 14H4.95312C3.55878 13.9999 2.50036 12.8948 2.5 11.6162V4.38477C2.50005 3.10595 3.55857 2.00008 4.95312 2H9.67969ZM4.95312 3.15332C4.32859 3.1534 3.75005 3.66694 3.75 4.38477V11.6162C3.75035 12.3337 4.32875 12.8466 4.95312 12.8467H11.0469C11.6711 12.8464 12.2496 12.3336 12.25 11.6162V6.46777H10.7412C9.68352 6.46742 8.89759 5.63135 8.89746 4.68555V3.15332H4.95312ZM10.1475 4.68555C10.1476 5.07024 10.4534 5.31407 10.7412 5.31445H12.1748C12.1123 5.1401 12.0135 4.98099 11.8828 4.85156L10.5156 3.5C10.4068 3.39244 10.2814 3.30891 10.1475 3.25098V4.68555Z"),
            )
        }.build()
        return _ic_document_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDocument16Preview() {
    Icon(
        imageVector = Icons.ic_document_16,
        contentDescription = null,
    )
}