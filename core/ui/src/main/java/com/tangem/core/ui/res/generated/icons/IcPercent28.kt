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

private var _ic_percent_28: ImageVector? = null

val Icons.ic_percent_28: ImageVector
    get() {
        if (_ic_percent_28 != null) return _ic_percent_28!!
        _ic_percent_28 = ImageVector.Builder(
            name = "ic_percent_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.3147 17.3155C19.0717 15.5586 21.9209 15.5587 23.678 17.3155C25.4347 19.0726 25.4348 21.9218 23.678 23.6788C21.921 25.4358 19.0718 25.4356 17.3147 23.6788C15.5579 21.9217 15.5577 19.0725 17.3147 17.3155ZM21.7581 18.9464C20.9728 18.3062 19.8151 18.3523 19.0833 19.0841C18.3025 19.8649 18.3025 21.1304 19.0833 21.9112C19.8641 22.6915 21.1298 22.6918 21.9104 21.9112C22.6422 21.1792 22.6875 20.0207 22.0471 19.2354L21.9104 19.0841L21.7581 18.9464Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.2366 4.9913C21.7246 4.50323 22.516 4.50343 23.0042 4.9913C23.4923 5.47945 23.4923 6.27072 23.0042 6.75888L6.75806 23.0059C6.26995 23.4938 5.47859 23.4939 4.99048 23.0059C4.50252 22.5178 4.50256 21.7265 4.99048 21.2384L21.2366 4.9913Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.31763 4.31747C6.07464 2.56089 8.92395 2.5608 10.6809 4.31747C12.4378 6.07442 12.4375 8.92361 10.6809 10.6808C8.92383 12.4379 6.07471 12.4379 4.31763 10.6808C2.56089 8.92362 2.56067 6.07446 4.31763 4.31747ZM8.76099 5.94833C7.97588 5.30821 6.81808 5.35453 6.08618 6.08603C5.30541 6.86681 5.30543 8.13238 6.08618 8.91317C6.86696 9.69379 8.13262 9.6939 8.91333 8.91317C9.64494 8.18106 9.69063 7.02252 9.05005 6.23739L8.91333 6.08603L8.76099 5.94833Z"),
            )
        }.build()
        return _ic_percent_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPercent28Preview() {
    Icon(
        imageVector = Icons.ic_percent_28,
        contentDescription = null,
    )
}