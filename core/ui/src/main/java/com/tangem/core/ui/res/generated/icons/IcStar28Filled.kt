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

private var _ic_star_28_filled: ImageVector? = null

val Icons.ic_star_28_filled: ImageVector
    get() {
        if (_ic_star_28_filled != null) return _ic_star_28_filled!!
        _ic_star_28_filled = ImageVector.Builder(
            name = "ic_star_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.0051 2.56738C15.1229 2.56744 16.1443 3.20085 16.6428 4.20117L18.7658 8.43359L23.466 9.10547C24.5851 9.26233 25.5172 10.0452 25.8635 11.1211C26.2092 12.196 25.9093 13.3735 25.093 14.1533L21.7209 17.3838L22.5207 21.9736C22.7158 23.0788 22.2667 24.1989 21.3615 24.8623C20.4557 25.5261 19.2508 25.618 18.2551 25.0986L14.0041 22.9023L9.75412 25.0967C8.75859 25.6159 7.55441 25.5249 6.64866 24.8613C5.74277 24.1973 5.29317 23.0757 5.48948 21.9697L6.2883 17.3838L2.91623 14.1543L2.91721 14.1533C2.10087 13.3735 1.80093 12.1961 2.1467 11.1211C2.49298 10.0453 3.42418 9.26236 4.54319 9.10547L9.24338 8.43359L11.3664 4.20117C11.8649 3.20058 12.8871 2.56738 14.0051 2.56738Z"),
            )
        }.build()
        return _ic_star_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcStar28FilledPreview() {
    Icon(
        imageVector = Icons.ic_star_28_filled,
        contentDescription = null,
    )
}