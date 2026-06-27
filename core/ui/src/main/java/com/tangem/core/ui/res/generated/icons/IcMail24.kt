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

private var _ic_mail_24: ImageVector? = null

val Icons.ic_mail_24: ImageVector
    get() {
        if (_ic_mail_24 != null) return _ic_mail_24!!
        _ic_mail_24 = ImageVector.Builder(
            name = "ic_mail_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.14258 8.48535C6.42676 8.01188 7.04112 7.85846 7.51465 8.14258L12 10.833L16.4854 8.14258C16.9589 7.85846 17.5732 8.01188 17.8574 8.48535C18.1415 8.95888 17.9881 9.57324 17.5146 9.85742L12.5146 12.8574C12.198 13.0474 11.802 13.0474 11.4854 12.8574L6.48535 9.85742C6.01188 9.57324 5.85846 8.95888 6.14258 8.48535Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19 4C20.6598 4 22 5.34813 22 7.00586V16.9951C22 18.6522 20.6595 20 19 20H5C3.34015 20 2 18.6519 2 16.9941V7.00586C2 5.34813 3.34015 4 5 4H19ZM5 6C4.44985 6 4 6.44757 4 7.00586V16.9941C4 17.5524 4.44985 18 5 18H19C19.5505 18 20 17.5521 20 16.9951V7.00586C20 6.44757 19.5502 6 19 6H5Z"),
            )
        }.build()
        return _ic_mail_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMail24Preview() {
    Icon(
        imageVector = Icons.ic_mail_24,
        contentDescription = null,
    )
}