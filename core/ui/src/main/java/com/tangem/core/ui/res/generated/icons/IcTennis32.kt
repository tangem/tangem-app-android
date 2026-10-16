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

private var _ic_tennis_32: ImageVector? = null

val Icons.ic_tennis_32: ImageVector
    get() {
        if (_ic_tennis_32 != null) return _ic_tennis_32!!
        _ic_tennis_32 = ImageVector.Builder(
            name = "ic_tennis_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M25.4736 21.4473C27.6972 21.4473 29.4999 23.25 29.5 25.4736C29.5 27.6974 27.6973 29.5 25.4736 29.5C23.25 29.4999 21.4473 27.6973 21.4473 25.4736C21.4474 23.25 23.2501 21.4473 25.4736 21.4473ZM25.4736 24.4473C24.9068 24.4473 24.4474 24.907 24.4473 25.4736C24.4473 26.0403 24.9067 26.4999 25.4736 26.5C26.0406 26.5 26.5 26.0403 26.5 25.4736C26.4999 24.907 26.0405 24.4473 25.4736 24.4473Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.334 2.5C20.9605 2.50004 23.4795 3.54332 25.3369 5.40039C27.1944 7.25773 28.2383 9.77758 28.2383 12.4043C28.2381 17.8739 23.8037 22.3075 18.334 22.3076H16.6807C15.1264 22.3077 13.6151 22.7298 12.3018 23.5117C12.2923 23.517 12.284 23.5243 12.2744 23.5293C11.6808 23.8858 11.1277 24.3157 10.6299 24.8135L8.02539 27.4189C7.50168 27.9426 6.79136 28.2372 6.05078 28.2373C5.31007 28.2373 4.59899 27.9427 4.0752 27.4189L3.31836 26.6611C2.79458 26.1374 2.5 25.4273 2.5 24.6865C2.50001 23.9458 2.79457 23.2357 3.31836 22.7119L5.92285 20.1064C7.52753 18.5018 8.42966 16.3259 8.42969 14.0566V12.4043C8.42969 9.77758 9.47262 7.25773 11.3301 5.40039C13.1875 3.54308 15.7072 2.49999 18.334 2.5ZM8.72754 21.4775C8.51081 21.7361 8.2844 21.9881 8.04395 22.2285L5.58496 24.6865L6.0498 25.1514L8.50879 22.6924C8.74922 22.452 9.00022 22.2245 9.25879 22.0078L8.72754 21.4775ZM10.6143 18.3115C10.5385 18.5028 10.458 18.692 10.3721 18.8789L11.8584 20.3643C12.0451 20.2785 12.2338 20.1977 12.4248 20.1221L10.6143 18.3115ZM18.334 5.5C16.5028 5.49999 14.746 6.22772 13.4512 7.52246C12.1566 8.81717 11.4297 10.5735 11.4297 12.4043V14.0566C11.4297 14.3234 11.4167 14.589 11.3984 14.8535L15.8828 19.3379C16.1476 19.3196 16.4136 19.3076 16.6807 19.3076H18.334C22.1471 19.3075 25.2381 16.2169 25.2383 12.4043C25.2383 10.5733 24.5106 8.8172 23.2158 7.52246C21.921 6.22775 20.1651 5.50004 18.334 5.5Z"),
            )
        }.build()
        return _ic_tennis_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTennis32Preview() {
    Icon(
        imageVector = Icons.ic_tennis_32,
        contentDescription = null,
    )
}