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

private var _ic_loading_spinner_20: ImageVector? = null

val Icons.ic_loading_spinner_20: ImageVector
    get() {
        if (_ic_loading_spinner_20 != null) return _ic_loading_spinner_20!!
        _ic_loading_spinner_20 = ImageVector.Builder(
            name = "ic_loading_spinner_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 2C11.5823 2 13.1287 2.46958 14.4443 3.34863C15.7599 4.22764 16.7851 5.47676 17.3906 6.93848C17.9961 8.40021 18.1553 10.0088 17.8467 11.5605C17.538 13.1124 16.776 14.5384 15.6572 15.6572C14.5384 16.776 13.1124 17.538 11.5605 17.8467C10.0088 18.1553 8.40021 17.9961 6.93848 17.3906C5.47676 16.7851 4.22764 15.7599 3.34863 14.4443C2.46958 13.1287 2 11.5823 2 10C2 9.58579 2.33579 9.25 2.75 9.25C3.16421 9.25 3.5 9.58579 3.5 10C3.5 11.2856 3.88147 12.5424 4.5957 13.6113C5.30993 14.6802 6.32505 15.5129 7.5127 16.0049C8.70042 16.4969 10.0077 16.6258 11.2686 16.375C12.5292 16.1241 13.6878 15.5056 14.5967 14.5967C15.5056 13.6878 16.1241 12.5292 16.375 11.2686C16.6258 10.0077 16.4969 8.70041 16.0049 7.5127C15.5129 6.32505 14.6802 5.30993 13.6113 4.5957C12.5424 3.88147 11.2856 3.5 10 3.5C9.58579 3.5 9.25 3.16421 9.25 2.75C9.25 2.33579 9.58579 2 10 2Z"),
            )
        }.build()
        return _ic_loading_spinner_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLoadingSpinner20Preview() {
    Icon(
        imageVector = Icons.ic_loading_spinner_20,
        contentDescription = null,
    )
}