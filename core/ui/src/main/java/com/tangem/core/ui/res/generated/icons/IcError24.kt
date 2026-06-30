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

private var _ic_error_24: ImageVector? = null

val Icons.ic_error_24: ImageVector
    get() {
        if (_ic_error_24 != null) return _ic_error_24!!
        _ic_error_24 = ImageVector.Builder(
            name = "ic_error_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1016 15.0049L12.1064 15.0059C12.7452 15.0603 13.25 15.5936 13.25 16.25C13.25 16.9403 12.6903 17.5 12 17.5C11.3529 17.5 10.8209 17.0081 10.7568 16.3779L10.75 16.25C10.749 15.593 11.2542 15.0608 11.8906 15.0059C11.9262 15.002 11.9624 15 11.999 15L12.1016 15.0049Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 6.5C12.5523 6.5 13 6.94772 13 7.5V12.5C13 13.0523 12.5523 13.5 12 13.5C11.4477 13.5 11 13.0523 11 12.5V7.5C11 6.94772 11.4477 6.5 12 6.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.1709 2C15.9664 2 16.7297 2.31661 17.292 2.87891L21.1191 6.70605L21.3193 6.92578C21.7579 7.45976 22 8.13194 22 8.82812V15.1709C22 15.9664 21.6834 16.7297 21.1211 17.292L16.707 21.707C16.5195 21.8946 16.2652 22 16 22H8.82812C8.03264 22 7.26935 21.6834 6.70703 21.1211L2.87891 17.293C2.31662 16.7306 2 15.9674 2 15.1719V8.82812C2 8.03264 2.31662 7.26935 2.87891 6.70703L6.70703 2.87891C7.26935 2.31662 8.03264 2 8.82812 2H15.1709ZM8.82812 4C8.56367 4 8.30876 4.10533 8.12109 4.29297L4.29297 8.12109C4.10533 8.30876 4 8.56367 4 8.82812V15.1719C4 15.4363 4.10533 15.6912 4.29297 15.8789L8.12109 19.707C8.30876 19.8947 8.56367 20 8.82812 20H15.5859L19.707 15.8779L19.7734 15.8047C19.9194 15.6266 20 15.4024 20 15.1709V8.82812C20 8.56383 19.8948 8.30943 19.707 8.12207L15.8779 4.29297C15.6903 4.10533 15.4354 4 15.1709 4H8.82812Z"),
            )
        }.build()
        return _ic_error_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcError24Preview() {
    Icon(
        imageVector = Icons.ic_error_24,
        contentDescription = null,
    )
}