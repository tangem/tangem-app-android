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

private var _ic_basketball_16: ImageVector? = null

val Icons.ic_basketball_16: ImageVector
    get() {
        if (_ic_basketball_16 != null) return _ic_basketball_16!!
        _ic_basketball_16 = ImageVector.Builder(
            name = "ic_basketball_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.75883 3.75886C6.10122 1.41648 9.89888 1.41648 12.2413 3.75886L12.4541 3.9825C13.4789 5.11666 13.9916 6.54794 13.9961 7.98054C13.9963 7.98703 13.9981 7.99354 13.9981 8.00008C13.9981 8.0063 13.9963 8.01245 13.9961 8.01863C13.9914 9.54745 13.4077 11.0748 12.2413 12.2413C9.89889 14.5837 6.10121 14.5836 3.75883 12.2413C1.41645 9.8989 1.41645 6.10124 3.75883 3.75886ZM5.11333 11.7696C5.79878 12.2959 6.59468 12.6086 7.41215 12.71C7.54106 11.5377 8.00693 10.4671 8.71196 9.59773L7.99907 8.88484L5.11333 11.7696ZM9.60258 10.4874C9.11856 11.1189 8.78989 11.8754 8.6729 12.7003C9.46006 12.5882 10.2234 12.2771 10.8848 11.7696L9.60258 10.4874ZM12.6983 8.67293C11.8737 8.79023 11.1176 9.11949 10.4864 9.60359L11.7686 10.8858C12.2766 10.2242 12.5861 9.46042 12.6983 8.67293ZM6.40141 7.28719C5.53184 7.99233 4.46169 8.45821 3.28911 8.58699C3.39035 9.40417 3.70379 10.1995 4.22954 10.8848L7.1143 8.00008L6.40141 7.28719ZM8.88383 8.00008L9.59672 8.71297C10.4666 8.00726 11.5378 7.541 12.711 7.41219C12.6095 6.59474 12.2948 5.79978 11.7686 5.11433L8.88383 8.00008ZM4.22954 5.11433C3.72212 5.7756 3.41104 6.53929 3.29887 7.32625C4.12401 7.20935 4.8801 6.88067 5.51176 6.39656L4.22954 5.11433ZM8.58696 3.28816C8.45828 4.4616 7.992 5.53242 7.28618 6.40242L7.99907 7.11531L10.8848 4.22957C10.1994 3.70379 9.40417 3.3894 8.58696 3.28816ZM7.32622 3.29988C6.53888 3.41212 5.77481 3.72266 5.11333 4.23054L6.39555 5.51277C6.87993 4.88113 7.20914 4.12512 7.32622 3.29988Z"),
            )
        }.build()
        return _ic_basketball_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBasketball16Preview() {
    Icon(
        imageVector = Icons.ic_basketball_16,
        contentDescription = null,
    )
}