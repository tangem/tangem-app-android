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

private var _ic_cards_star_28: ImageVector? = null

val Icons.ic_cards_star_28: ImageVector
    get() {
        if (_ic_cards_star_28 != null) return _ic_cards_star_28!!
        _ic_cards_star_28 = ImageVector.Builder(
            name = "ic_cards_star_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.8528 8.41699C18.543 8.41714 19.1028 8.97673 19.1028 9.66699C19.1033 10.1723 19.5134 10.582 20.0188 10.582C20.7089 10.5822 21.2687 11.1419 21.2688 11.832C21.2685 12.522 20.7088 13.0818 20.0188 13.082C19.5446 13.082 19.1545 13.4425 19.1076 13.9043L19.1028 13.998L19.0969 14.126C19.0326 14.7559 18.4996 15.2479 17.8528 15.248C17.1627 15.2479 16.6031 14.6881 16.6028 13.998C16.6028 13.7555 16.5065 13.5222 16.3352 13.3506C16.1636 13.179 15.9303 13.0822 15.6877 13.082C14.9975 13.082 14.438 12.5222 14.4377 11.832C14.4378 11.1848 14.9294 10.6527 15.5598 10.5889L15.7805 10.5771C16.242 10.5303 16.6023 10.1407 16.6028 9.66699C16.6028 8.9767 17.1625 8.41709 17.8528 8.41699Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.6789 2.99805C23.5137 2.99845 25.0012 4.48737 25.0012 6.32227V17.3428C25.0008 19.1773 23.5135 20.6646 21.6789 20.665H19.9543C19.8884 20.9023 19.7956 21.1338 19.6701 21.3506C19.2808 22.0229 18.6395 22.5117 17.8889 22.7119L17.8899 22.7129L9.75412 24.9014L9.75315 24.9023C8.1878 25.3207 6.57853 24.3929 6.15451 22.8291L6.15354 22.8262L3.09885 11.4297L3.0969 11.4258C2.68407 9.86287 3.6082 8.25933 5.16721 7.83203L5.17502 7.8291L10.7053 6.35449V6.32227C10.7053 4.48711 12.1934 2.99805 14.0285 2.99805H21.6789ZM5.82736 10.2422C5.59147 10.3071 5.45237 10.5505 5.51486 10.7871L8.5676 22.1758L8.59885 22.2588C8.6777 22.4154 8.84376 22.5113 9.01877 22.501L9.10764 22.4863L15.8791 20.665H14.0285C12.1937 20.665 10.7057 19.1776 10.7053 17.3428V8.94141L5.82736 10.2422ZM14.0285 5.49902C13.5741 5.49902 13.2053 5.86783 13.2053 6.32227V17.3428C13.2057 17.7968 13.5744 18.165 14.0285 18.165H21.6789C22.1328 18.1646 22.5008 17.7966 22.5012 17.3428V6.32227C22.5012 5.86808 22.133 5.49943 21.6789 5.49902H14.0285Z"),
            )
        }.build()
        return _ic_cards_star_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCardsStar28Preview() {
    Icon(
        imageVector = Icons.ic_cards_star_28,
        contentDescription = null,
    )
}