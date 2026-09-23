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

private var _ic_user_24: ImageVector? = null

val Icons.ic_user_24: ImageVector
    get() {
        if (_ic_user_24 != null) return _ic_user_24!!
        _ic_user_24 = ImageVector.Builder(
            name = "ic_user_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.5684 14.6255C17.6529 14.6255 19.9987 17.1021 19.999 20.0562C19.999 20.6084 19.5513 21.0562 18.999 21.0562C18.4471 21.0558 17.999 20.6082 17.999 20.0562C17.9987 18.177 16.5188 16.6255 14.5684 16.6255H9.43262C7.55322 16.6256 6.00127 18.1061 6.00098 20.0562C6.00098 20.6084 5.55326 21.0562 5.00098 21.0562C4.4488 21.056 4.00098 20.6084 4.00098 20.0562C4.00127 16.9718 6.47857 14.6256 9.43262 14.6255H14.5684Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.27539 4.42431C10.3724 2.451 13.5915 2.45102 15.6885 4.42431C15.6912 4.42691 15.6936 4.43044 15.6963 4.4331C15.7005 4.43717 15.7058 4.44071 15.71 4.44482C17.8001 6.53489 17.8319 9.90468 15.6885 11.9224C13.6328 13.857 10.2751 14.0497 8.23242 11.8794C6.29804 9.82379 6.10513 6.4668 8.27539 4.42431ZM14.3037 5.86767C12.9766 4.63205 10.9678 4.63608 9.64551 5.88037C8.39199 7.06017 8.40085 9.14096 9.68848 10.5093C10.8684 11.7628 12.949 11.754 14.3174 10.4663C15.5949 9.26385 15.6292 7.20156 14.3037 5.86767Z"),
            )
        }.build()
        return _ic_user_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser24Preview() {
    Icon(
        imageVector = Icons.ic_user_24,
        contentDescription = null,
    )
}