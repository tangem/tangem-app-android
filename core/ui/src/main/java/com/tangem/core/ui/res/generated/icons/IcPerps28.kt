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

private var _ic_perps_28: ImageVector? = null

val Icons.ic_perps_28: ImageVector
    get() {
        if (_ic_perps_28 != null) return _ic_perps_28!!
        _ic_perps_28 = ImageVector.Builder(
            name = "ic_perps_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.8643 11.3682C16.3182 9.91422 18.6773 9.91302 20.1309 11.3682L20.1299 11.3691C21.5836 12.8235 21.584 15.1797 20.1299 16.6338C18.7211 18.0421 16.4656 18.086 15.0039 16.7656L14.8643 16.6338L13.999 15.7676L13.1348 16.6328C11.6808 18.0865 9.32164 18.0868 7.86816 16.6318L7.86914 16.6309C6.41586 15.1766 6.41539 12.8212 7.86914 11.3672C9.32345 9.91288 11.6805 9.91288 13.1348 11.3672L13.999 12.2324L14.8643 11.3682ZM11.3672 13.1348C10.8892 12.6568 10.1147 12.6568 9.63672 13.1348C9.15897 13.6128 9.1588 14.3873 9.63672 14.8652C10.113 15.3419 10.889 15.3432 11.3672 14.8652L12.2314 14L11.3672 13.1348ZM18.3623 13.1357C17.8859 12.6588 17.1101 12.6574 16.6318 13.1357L15.7666 14L16.6328 14.8662C17.1107 15.3438 17.8844 15.3437 18.3623 14.8662C18.8104 14.4181 18.8384 13.7092 18.4463 13.2285L18.3623 13.1357Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2.00195C20.6262 2.00222 25.9989 7.3738 25.999 14C25.9989 20.6262 20.6262 25.9988 14 25.999C7.37372 25.9989 2.00111 20.6263 2.00098 14C2.00111 7.37372 7.37372 2.00209 14 2.00195ZM14 4.50195C8.75443 4.50209 4.50209 8.75443 4.50195 14C4.50209 19.2456 8.75443 23.4989 14 23.499C19.2455 23.4988 23.4989 19.2455 23.499 14C23.4989 8.75451 19.2455 4.50222 14 4.50195Z"),
            )
        }.build()
        return _ic_perps_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPerps28Preview() {
    Icon(
        imageVector = Icons.ic_perps_28,
        contentDescription = null,
    )
}