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

private var _ic_loading_spinner_24: ImageVector? = null

val Icons.ic_loading_spinner_24: ImageVector
    get() {
        if (_ic_loading_spinner_24 != null) return _ic_loading_spinner_24!!
        _ic_loading_spinner_24 = ImageVector.Builder(
            name = "ic_loading_spinner_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C13.9778 2 15.9112 2.58673 17.5557 3.68555C19.2001 4.78434 20.4824 6.34567 21.2393 8.17285C21.9961 10.0001 22.1935 12.0114 21.8076 13.9512C21.4217 15.8909 20.4697 17.6728 19.0713 19.0713C17.6728 20.4697 15.8909 21.4217 13.9512 21.8076C12.0114 22.1935 10.0001 21.9961 8.17285 21.2393C6.34567 20.4824 4.78434 19.2001 3.68555 17.5557C2.58673 15.9112 2 13.9778 2 12C2 11.4477 2.44772 11 3 11C3.55228 11 4 11.4477 4 12C4 13.5823 4.46958 15.1287 5.34863 16.4443C6.22764 17.7599 7.47676 18.7851 8.93848 19.3906C10.4002 19.9961 12.0088 20.1553 13.5605 19.8467C15.1124 19.538 16.5384 18.776 17.6572 17.6572C18.776 16.5384 19.538 15.1124 19.8467 13.5605C20.1553 12.0088 19.9961 10.4002 19.3906 8.93848C18.7851 7.47676 17.7599 6.22764 16.4443 5.34863C15.1287 4.46958 13.5823 4 12 4C11.4477 4 11 3.55228 11 3C11 2.44772 11.4477 2 12 2Z"),
            )
        }.build()
        return _ic_loading_spinner_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLoadingSpinner24Preview() {
    Icon(
        imageVector = Icons.ic_loading_spinner_24,
        contentDescription = null,
    )
}