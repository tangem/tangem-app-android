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

private var _ic_star_20_filled: ImageVector? = null

val Icons.ic_star_20_filled: ImageVector
    get() {
        if (_ic_star_20_filled != null) return _ic_star_20_filled!!
        _ic_star_20_filled = ImageVector.Builder(
            name = "ic_star_20_filled",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.0001 2.37305C10.7193 2.3731 11.3762 2.78133 11.6964 3.4248L13.1486 6.32617L16.3702 6.78809C17.0898 6.88943 17.687 7.39336 17.9093 8.08496C18.1313 8.77628 17.9381 9.53342 17.4132 10.0352L17.4142 10.0361L15.1036 12.252L15.6525 15.4014C15.7776 16.1118 15.4891 16.8328 14.9073 17.2598C14.3258 17.6865 13.552 17.745 12.9122 17.4121L9.99915 15.9062L7.08801 17.4121L7.08704 17.4111C6.44739 17.7437 5.6743 17.6853 5.0929 17.2588C4.51147 16.832 4.22302 16.1114 4.34778 15.4014L4.89563 12.252L2.58606 10.0361L2.58704 10.0352C2.06209 9.53341 1.86894 8.7763 2.09094 8.08496C2.31313 7.39361 2.91084 6.88968 3.63001 6.78809L6.85071 6.32617L8.30383 3.4248C8.62404 2.78129 9.28092 2.37305 10.0001 2.37305Z"),
            )
        }.build()
        return _ic_star_20_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcStar20FilledPreview() {
    Icon(
        imageVector = Icons.ic_star_20_filled,
        contentDescription = null,
    )
}