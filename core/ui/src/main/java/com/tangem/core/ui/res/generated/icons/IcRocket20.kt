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

private var _ic_rocket_20: ImageVector? = null

val Icons.ic_rocket_20: ImageVector
    get() {
        if (_ic_rocket_20 != null) return _ic_rocket_20!!
        _ic_rocket_20 = ImageVector.Builder(
            name = "ic_rocket_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.12354 12.4004C5.80658 12.2978 6.49843 12.5253 6.98683 13.0137C7.47503 13.502 7.70264 14.1931 7.60011 14.876C7.45789 15.8208 6.71587 16.563 5.77101 16.7051L3.86085 16.9922C3.62582 17.0273 3.38736 16.9492 3.21925 16.7812C3.05111 16.6131 2.97316 16.3738 3.00831 16.1387L3.29444 14.2295C3.43639 13.2846 4.1788 12.5428 5.12354 12.4004ZM5.34718 13.8838C5.05358 13.9281 4.82196 14.1585 4.77784 14.4521L4.64112 15.3574L5.54835 15.2217C5.84179 15.1774 6.07237 14.9467 6.11671 14.6533C6.1486 14.4411 6.07798 14.226 5.92628 14.0742C5.77451 13.9225 5.55943 13.852 5.34718 13.8838Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.7036 3.00781C16.4319 3.08187 17.0004 3.69657 17.0005 4.44434C17.0005 6.86673 15.8989 9.15759 14.0073 10.6709L13.5278 11.0537V13.7373C13.5278 14.2844 13.2183 14.7846 12.729 15.0293L11.0513 15.8682C10.6768 16.0552 10.2388 16.0713 9.85206 15.9111C9.46553 15.7508 9.16704 15.4302 9.03468 15.0332L8.61671 13.7793C8.51691 13.7195 8.42207 13.6481 8.33644 13.5625L6.43702 11.6641C6.35088 11.5779 6.27924 11.4824 6.21925 11.3818L4.96632 10.9648C4.5695 10.8323 4.24955 10.5339 4.08937 10.1475C3.92926 9.76093 3.94454 9.32353 4.13136 8.94922L4.97022 7.27148L5.07374 7.09668C5.34072 6.70974 5.78358 6.47284 6.26222 6.47266H8.94483L9.32862 5.99316C10.8419 4.10153 13.1337 3 15.5562 3L15.7036 3.00781ZM10.2593 13.6699C10.2338 13.6903 10.2067 13.7082 10.1802 13.7266L10.437 14.4971L12.0278 13.7021V12.2539L10.2593 13.6699ZM15.4985 4.50098C13.553 4.51814 11.7172 5.40894 10.5005 6.92969L7.53272 10.6387L9.36085 12.4668L13.0708 9.5C14.5917 8.28313 15.4816 6.44671 15.4985 4.50098ZM5.50147 9.56152L6.27296 9.81836C6.29134 9.79197 6.31022 9.76568 6.33058 9.74023L7.74464 7.97266H6.29737L5.50147 9.56152Z"),
            )
        }.build()
        return _ic_rocket_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcRocket20Preview() {
    Icon(
        imageVector = Icons.ic_rocket_20,
        contentDescription = null,
    )
}