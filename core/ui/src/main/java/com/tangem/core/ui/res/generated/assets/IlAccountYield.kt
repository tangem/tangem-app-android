@file:Suppress("all")

package com.tangem.core.ui.res.generated.assets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.generated.icons.Icons

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _il_account_yield: ImageVector? = null

val Icons.il_account_yield: ImageVector
    get() {
        if (_il_account_yield != null) return _il_account_yield!!
        _il_account_yield = ImageVector.Builder(
            name = "il_account_yield",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0090F9)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M30.5002 17.2499C30.5002 16.0073 31.5075 15 32.7501 15C33.9927 15 35 16.0073 35 17.2499V31.4992C35 32.7418 33.9927 33.7491 32.7501 33.7491C31.5075 33.7491 30.5002 32.7418 30.5002 31.4992V17.2499Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0090F9)),
                fillAlpha = 0.6f,
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M22.2495 22.4999C22.2495 21.2573 23.2568 20.25 24.4994 20.25C25.742 20.25 26.7493 21.2573 26.7493 22.4999V31.4994C26.7493 32.742 25.742 33.7493 24.4994 33.7493C23.2568 33.7493 22.2495 32.742 22.2495 31.4994V22.4999Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0090F9)),
                fillAlpha = 0.4f,
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 26.9996C14 25.7571 15.0073 24.7498 16.2499 24.7498C17.4925 24.7498 18.4998 25.7571 18.4998 26.9996V31.4994C18.4998 32.742 17.4925 33.7493 16.2499 33.7493C15.0073 33.7493 14 32.742 14 31.4994V26.9996Z"),
            )
        }.build()
        return _il_account_yield!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountYieldPreview() {
    Icon(
        imageVector = Icons.il_account_yield,
        contentDescription = null,
    )
}