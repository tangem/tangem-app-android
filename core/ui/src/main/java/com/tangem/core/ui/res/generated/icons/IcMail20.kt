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

private var _ic_mail_20: ImageVector? = null

val Icons.ic_mail_20: ImageVector
    get() {
        if (_ic_mail_20 != null) return _ic_mail_20!!
        _ic_mail_20 = ImageVector.Builder(
            name = "ic_mail_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.6484 6.95996C14.0043 6.74868 14.464 6.86514 14.6758 7.2207C14.8873 7.57655 14.7707 8.03721 14.415 8.24902L10.3857 10.6455C10.1496 10.7858 9.85527 10.7857 9.61914 10.6455L5.58984 8.24902C5.2341 8.0372 5.11744 7.5766 5.3291 7.2207C5.54094 6.865 6.00154 6.74831 6.35742 6.95996L10.002 9.12695L13.6484 6.95996Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.6436 3.5C16.9633 3.50042 18.0049 4.58896 18.0049 5.89746V14.1045C18.0048 15.3306 17.0892 16.3635 15.8877 16.4883L15.6436 16.501H4.3623C3.04231 16.5009 2.00002 15.4122 2 14.1035V5.89746C2.00001 4.58878 3.04231 3.50012 4.3623 3.5H15.6436ZM4.3623 5C3.90175 5.00012 3.50099 5.38584 3.50098 5.89746V14.1035C3.50099 14.6151 3.90175 15.0009 4.3623 15.001H15.6436C16.1041 15.0006 16.5048 14.6148 16.5049 14.1045V5.89746C16.5049 5.38605 16.1039 5.00042 15.6436 5H4.3623Z"),
            )
        }.build()
        return _ic_mail_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMail20Preview() {
    Icon(
        imageVector = Icons.ic_mail_20,
        contentDescription = null,
    )
}