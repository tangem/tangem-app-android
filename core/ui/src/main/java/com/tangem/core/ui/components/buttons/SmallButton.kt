package com.tangem.core.ui.components.buttons

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

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
    val icon: TangemButtonIconPosition = TangemButtonIconPosition.None,
    val enabled: Boolean = true,
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

@Suppress("LongMethod")
@Composable
private fun SmallButton(config: SmallButtonConfig, isPrimary: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size = TangemTheme.dimens.radius16)

    val backgroundColor by animateColorAsState(
        targetValue = if (isPrimary) TangemTheme.colors.button.primary else TangemTheme.colors.button.secondary,
        label = "Update background color",
    )

    Row(
        modifier = modifier
            .defaultMinSize(
                minWidth = TangemTheme.dimens.size46,
                minHeight = TangemTheme.dimens.size24,
            )
            .clip(shape)
            .background(
                color = backgroundColor,
                shape = shape,
            )
            .clickable(enabled = config.enabled, onClick = config.onClick)
            .padding(
                paddingValues = when (config.icon) {
                    is TangemButtonIconPosition.None -> PaddingValues(
                        horizontal = TangemTheme.dimens.spacing12,
                    )
                    is TangemButtonIconPosition.End -> PaddingValues(
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing8,
                    )
                    is TangemButtonIconPosition.Start -> PaddingValues(
                        start = TangemTheme.dimens.spacing8,
                        end = TangemTheme.dimens.spacing12,
                    )
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        ContentContainer(
            iconPosition = config.icon,
            text = {
                val textColor by animateColorAsState(
                    targetValue = when {
                        !config.enabled -> TangemTheme.colors.text.disabled
                        isPrimary -> TangemTheme.colors.text.primary2
                        else -> TangemTheme.colors.text.primary1
                    },
                    label = "Update text color",
                )

                Text(
                    modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing4),
                    text = config.text.resolveReference(),
                    color = textColor,
                    maxLines = 1,
                    style = TangemTheme.typography.button,
                )
            },
            icon = { iconResId ->
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size16),
                    painter = painterResource(id = iconResId),
                    tint = if (config.enabled) {
                        TangemTheme.colors.icon.secondary
                    } else {
                        TangemTheme.colors.icon.inactive
                    },
                    contentDescription = null,
                )
            },
        )
    }
}

@Composable
private fun RowScope.ContentContainer(
    iconPosition: TangemButtonIconPosition,
    text: @Composable RowScope.() -> Unit,
    icon: @Composable RowScope.(Int) -> Unit,
) {
    if (iconPosition is TangemButtonIconPosition.Start) {
        icon(iconPosition.iconResId)
        Spacer(modifier = Modifier.requiredWidth(TangemTheme.dimens.spacing4))
    }
    text()
    if (iconPosition is TangemButtonIconPosition.End) {
        Spacer(modifier = Modifier.requiredWidth(TangemTheme.dimens.spacing4))
        icon(iconPosition.iconResId)
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
        SecondarySmallButton(
            config = config.copy(
                text = TextReference.Str(value = "Rating"),
                icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_chevron_24),
            ),
        )
        SecondarySmallButton(
            config = config.copy(
                text = TextReference.Str(value = "Add token"),
                icon = TangemButtonIconPosition.Start(iconResId = R.drawable.ic_plus_24),
            ),
        )
        SecondarySmallButton(
            config = config.copy(
                text = TextReference.Str(value = "Add token"),
                icon = TangemButtonIconPosition.Start(iconResId = R.drawable.ic_plus_24),
                enabled = false,
            ),
        )
    }
}