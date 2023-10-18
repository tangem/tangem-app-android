package com.tangem.core.ui.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
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
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = TangemTheme.dimens.size46, minHeight = TangemTheme.dimens.size24)
            .clip(shape)
            .background(
                color = if (isPrimary) TangemTheme.colors.button.primary else TangemTheme.colors.button.secondary,
                shape = shape,
            )
            .clickable(enabled = true, onClick = config.onClick)
            .padding(
                vertical = TangemTheme.dimens.spacing2,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = config.text.resolveReference(),
            color = if (isPrimary) TangemTheme.colors.text.primary2 else TangemTheme.colors.text.primary1,
            maxLines = 1,
            style = TangemTheme.typography.button,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_SmallButton_Light() {
    TangemTheme(isDark = false) {
        ButtonsSample()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_SmallButton_Dark() {
    TangemTheme(isDark = true) {
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
            text = TextReference.Str(value = "Add"),
            onClick = {},
        )
        PrimarySmallButton(config = config)
        SecondarySmallButton(config = config.copy(text = TextReference.Str(value = "Add")))
    }
}