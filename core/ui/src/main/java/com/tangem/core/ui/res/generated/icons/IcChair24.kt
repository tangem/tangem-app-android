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

private var _ic_chair_24: ImageVector? = null

val Icons.ic_chair_24: ImageVector
    get() {
        if (_ic_chair_24 != null) return _ic_chair_24!!
        _ic_chair_24 = ImageVector.Builder(
            name = "ic_chair_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.9999 2C18.209 2 19.9999 3.79086 19.9999 6V10.1553C20.5375 10.3371 21.0181 10.6728 21.3759 11.1309C21.933 11.8445 22.1306 12.775 21.911 13.6533L20.8925 17.7275C20.7448 18.3181 20.4266 18.8296 19.9999 19.2178V21C19.9998 21.5522 19.5521 22 18.9999 22C18.4477 22 18 21.5522 17.9999 21V19.999C17.994 19.9991 17.9882 20 17.9823 20H6.01745C6.01163 20 6.00569 19.9991 5.99988 19.999V21C5.99977 21.5522 5.5521 22 4.99988 22C4.44766 22 3.99998 21.5522 3.99988 21V19.2178C3.57306 18.8296 3.25496 18.3182 3.1073 17.7275L2.08874 13.6533C1.86917 12.775 2.06671 11.8445 2.6239 11.1309C2.98166 10.6727 3.46216 10.3371 3.99988 10.1553V6C3.99988 3.79086 5.79074 2 7.99988 2H15.9999ZM19.0585 12C18.6272 12.0001 18.2511 12.2936 18.1464 12.7119L17.5135 15.2422C17.4023 15.6873 17.0026 15.9999 16.5438 16H7.45593C6.99714 15.9999 6.59749 15.6873 6.4862 15.2422L5.85339 12.7119C5.74867 12.2936 5.37254 12.0001 4.94128 12C4.65185 12 4.37827 12.1333 4.20007 12.3613C4.02194 12.5895 3.95906 12.8872 4.02917 13.168L5.04773 17.2422C5.15901 17.6873 5.55867 17.9999 6.01745 18H17.9823C18.4411 17.9999 18.8408 17.6873 18.952 17.2422L19.9706 13.168C20.0407 12.8872 19.9778 12.5895 19.7997 12.3613C19.6215 12.1333 19.3479 12 19.0585 12ZM7.99988 4C6.89531 4 5.99988 4.89543 5.99988 6V10.1992C6.87212 10.5365 7.55657 11.2785 7.79382 12.2275L8.23718 14H15.7626L16.2059 12.2275C16.4432 11.2784 17.1275 10.5365 17.9999 10.1992V6C17.9999 4.89543 17.1044 4 15.9999 4H7.99988Z"),
            )
        }.build()
        return _ic_chair_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChair24Preview() {
    Icon(
        imageVector = Icons.ic_chair_24,
        contentDescription = null,
    )
}