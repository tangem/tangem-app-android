package com.tangem.core.ui.components.buttons

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme

/**
 * Small button config
 *
 * @property text      text
 * @property onClick   lambda be invoked when action component is clicked
 *
 */
data class SmallButtonConfig(
    val text: TextReference,
    val onClick: () -> Unit,
)

/**
 * Primary small button
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=68%3A20&mode=design&t=fjwUkRtMUA4Q4s5r-1)
 *
 * @property config Config, containing parameters for a button
 */
@Composable
fun PrimarySmallButton(config: SmallButtonConfig, modifier: Modifier = Modifier) {
    SmallButton(config = config, isPrimary = true, modifier = modifier)
}

/**
 * Secondary small button
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=68%3A20&mode=design&t=fjwUkRtMUA4Q4s5r-1)
 *
 * @property config Config, containing parameters for a button
 */
@Composable
fun SecondarySmallButton(config: SmallButtonConfig, modifier: Modifier = Modifier) {
    SmallButton(config = config, isPrimary = false, modifier = modifier)
}

@Composable
private fun SmallButton(config: SmallButtonConfig, isPrimary: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size = TangemTheme.dimens.radius16)

    val backgroundColor by animateColorAsState(
        targetValue = if (isPrimary) TangemTheme.colors.button.primary else TangemTheme.colors.button.secondary,
        label = "Update background color",
    )

    val textColor by animateColorAsState(
        targetValue = if (isPrimary) TangemTheme.colors.text.primary2 else TangemTheme.colors.text.primary1,
        label = "Update text color",
    )
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = TangemTheme.dimens.size46, minHeight = TangemTheme.dimens.size24)
            .clip(shape)
            .background(
                color = backgroundColor,
                shape = shape,
            )
            .clickable(enabled = true, onClick = config.onClick)
            .padding(
                vertical = TangemTheme.dimens.spacing2,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = TangemTheme.dimens.spacing10,
            ),
            text = config.text.resolveReference(),
            color = textColor,
            maxLines = 1,
            style = TangemTheme.typography.button,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_SmallButton() {
    TangemThemePreview {
        ButtonsSample()
    }
}

@Composable
private fun ButtonsSample() {
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        val config = SmallButtonConfig(
            text = TextReference.Str(value = "Adddddddd"),
            onClick = {},
        )
        PrimarySmallButton(config = config)
        SecondarySmallButton(config = config.copy(text = TextReference.Str(value = "Add")))
    }
}