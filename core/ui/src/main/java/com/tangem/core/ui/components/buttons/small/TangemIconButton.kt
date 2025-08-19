package com.tangem.core.ui.components.buttons.small

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Small button icon
 *
 * @param iconRes       icon resource
 * @param onClick       lambda be invoked when action component is clicked
 * @param modifier      composable modifier
 * @param shape         icon button shape
 * @param background    background color
 * @param iconTint      icon color
 *
 * [Show in Figma](https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=4105-1439&t=nnYBX1qCZmUNhBDf-4)
 */
@Composable
fun TangemIconButton(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    background: Color = TangemTheme.colors.button.secondary,
    iconTint: Color = TangemTheme.colors.icon.secondary,
) {
    Icon(
        painter = rememberVectorPainter(ImageVector.vectorResource(iconRes)),
        contentDescription = "",
        tint = iconTint,
        modifier = modifier
            .size(24.dp)
            .clip(shape)
            .background(background)
            .padding(4.dp)
            .clickable(
                onClick = onClick,
            ),
    )
}

// region Preview
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemIconButton_Preview() {
    TangemThemePreview {
        TangemIconButton(
            iconRes = R.drawable.ic_close_24,
            onClick = {},
        )
    }
}
// endregion