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

private var _il_account_letter: ImageVector? = null

val Icons.il_account_letter: ImageVector
    get() {
        if (_il_account_letter != null) return _il_account_letter!!
        _il_account_letter = ImageVector.Builder(
            name = "il_account_letter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.9668 13.5625C16.9666 14.2695 16.3578 14.8025 15.542 14.8027C14.9436 14.8027 14.5138 14.5035 14.5137 13.9922C14.5137 13.4971 14.8669 13.2199 15.623 13.1709L16.9668 13.0781V13.5625Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.87793 9.36719L9.94434 12.6318H7.70801L8.78516 9.36719H8.87793Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.001 1.71484C16.8494 1.71484 19.274 1.71445 20.7803 3.2207C22.2865 4.72697 22.2861 7.15156 22.2861 12C22.2861 16.8487 22.2866 19.274 20.7803 20.7803C19.274 22.2865 16.8494 22.2861 12.001 22.2861C7.15224 22.2861 4.72701 22.2866 3.2207 20.7803C1.7144 19.274 1.71484 16.8487 1.71484 12C1.71484 7.15162 1.7145 4.72697 3.2207 3.2207C4.72701 1.7144 7.15224 1.71484 12.001 1.71484ZM15.9111 9.68262C14.4968 9.68274 13.5122 10.3467 13.4033 11.3857H14.6768C14.802 10.9778 15.2265 10.7549 15.8467 10.7549C16.5755 10.755 16.9668 11.0868 16.9668 11.6797V12.1914L15.4326 12.2832C13.9854 12.3648 13.1748 12.9912 13.1748 14.0576C13.175 15.1456 13.9966 15.8418 15.1445 15.8418C15.9225 15.8417 16.5754 15.5044 16.9072 14.9277H17V15.7441H18.2998V11.6797C18.2998 10.4229 17.44 9.68262 15.9111 9.68262ZM8.07324 7.89258L5.28711 15.7441H6.70703L7.38184 13.7148H10.2705L10.9346 15.7441H12.4414L9.65039 7.89258H8.07324Z"),
            )
        }.build()
        return _il_account_letter!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountLetterPreview() {
    Icon(
        imageVector = Icons.il_account_letter,
        contentDescription = null,
    )
}