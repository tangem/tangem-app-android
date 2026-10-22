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

private var _ic_hockey_12: ImageVector? = null

val Icons.ic_hockey_12: ImageVector
    get() {
        if (_ic_hockey_12 != null) return _ic_hockey_12!!
        _ic_hockey_12 = ImageVector.Builder(
            name = "ic_hockey_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.90464 1.20326C6.7952 0.906539 7.76931 0.879149 8.40366 1.4972C8.40913 1.50221 8.41594 1.50655 8.42124 1.51185C8.42466 1.51527 8.42762 1.51915 8.431 1.52259L10.4876 3.57923C11.121 4.21319 11.0954 5.19717 10.7962 6.09486C10.4864 7.02453 9.83605 8.03747 8.93686 8.93665C8.03768 9.83584 7.02474 10.4862 6.09507 10.796C5.19738 11.0952 4.21338 11.1208 3.57944 10.4874L1.51206 8.42005C0.878385 7.78616 0.904337 6.80222 1.20346 5.90443C1.51328 4.97469 2.16358 3.96189 3.06284 3.06263C3.96209 2.16338 4.9749 1.51307 5.90464 1.20326ZM8.88803 3.39369C8.85286 3.60579 8.79924 3.81918 8.72983 4.02747C8.41999 4.95715 7.76965 5.97008 6.87046 6.86927C5.97127 7.76846 4.95834 8.4188 4.02866 8.72865C3.81974 8.79826 3.60565 8.85168 3.39292 8.88685L4.28647 9.7804C4.50849 10.0018 4.99401 10.1083 5.77866 9.84681C6.53216 9.59568 7.41668 9.04277 8.22983 8.22962C9.04297 7.41648 9.59588 6.53195 9.84702 5.77845C10.1085 4.99382 10.002 4.5083 9.78061 4.28626L8.88803 3.39369ZM7.71323 2.21888C7.49137 1.99726 7.00597 1.89098 6.22104 2.15247C5.46749 2.40358 4.58309 2.95644 3.76987 3.76966C2.95665 4.58288 2.40379 5.46728 2.15268 6.22083C1.89201 7.00329 1.99694 7.48728 2.21714 7.71009L2.22007 7.71302C2.44206 7.93447 2.92755 8.04089 3.71225 7.77943C4.46576 7.52829 5.35028 6.97539 6.16342 6.16224C6.97657 5.34909 7.52947 4.46457 7.78061 3.71107C8.04035 2.93154 7.93658 2.44775 7.71811 2.22376L7.71323 2.21888Z"),
            )
        }.build()
        return _ic_hockey_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHockey12Preview() {
    Icon(
        imageVector = Icons.ic_hockey_12,
        contentDescription = null,
    )
}