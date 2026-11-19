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

private var _il_account_staking: ImageVector? = null

val Icons.il_account_staking: ImageVector
    get() {
        if (_il_account_staking != null) return _il_account_staking!!
        _il_account_staking = ImageVector.Builder(
            name = "il_account_staking",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0090F9)),
                fillAlpha = 0.9f,
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.0377 22.322C11.6541 21.4276 11.6541 19.4036 13.0377 18.5092L22.3568 12.4849C23.3569 11.8384 24.6431 11.8384 25.6432 12.4849L34.9623 18.5092C36.3459 19.4036 36.3459 21.4276 34.9623 22.322L26.054 28.0808C24.8039 28.8889 23.1961 28.8889 21.946 28.0808L13.0377 22.322Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0090F9)),
                fillAlpha = 0.3f,
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M34.9655 25.8224C36.3486 26.7168 36.3488 28.7413 34.9655 29.6356L26.0569 35.3942C24.8068 36.2023 23.1984 36.2023 21.9483 35.3942L13.0397 29.6356C11.6565 28.7413 11.6566 26.7168 13.0397 25.8224L15.7456 24.0728L21.9472 28.0806C23.1972 28.8887 24.8057 28.8887 26.0558 28.0806L32.2573 24.0728L34.9655 25.8224Z"),
            )
        }.build()
        return _il_account_staking!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountStakingPreview() {
    Icon(
        imageVector = Icons.il_account_staking,
        contentDescription = null,
    )
}