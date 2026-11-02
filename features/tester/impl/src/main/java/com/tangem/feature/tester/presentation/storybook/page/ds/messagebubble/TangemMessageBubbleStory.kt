@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.messagebubble

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_16
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_16_filled
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBubbleStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBubbleStory.Background
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBubbleStory.TrailingButton

private const val SHORT_TEXT = "Earn up to 4.5% APY"
private const val LONG_TEXT = "Stake your ETH to earn up to 4.5% APY. Rewards are paid out daily and you can " +
    "unstake at any time."

@Composable
internal fun TangemMessageBubbleStory(state: TangemMessageBubbleStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Preview stays pinned at the top.
        ComponentPreview(state = state)
        // Only the controls scroll.
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(label = "Variant") {
                ChipGrid(
                    items = TangemMessageBubble.Variant.entries,
                    label = { it.name },
                    isSelected = { it == state.variant },
                    onSelect = state.onVariantChange,
                )
            }
            Section(label = "Message") {
                ChipGrid(
                    items = TangemMessageBubbleStory.Message.entries,
                    label = { it.label },
                    isSelected = { it == state.message },
                    onSelect = state.onMessageChange,
                )
            }
            Section(label = "Trailing button") {
                ChipGrid(
                    items = TrailingButton.entries,
                    label = { it.label },
                    isSelected = { it == state.trailingButton },
                    onSelect = state.onTrailingButtonChange,
                )
            }
            Section(label = "Background") {
                ChipGrid(
                    items = Background.entries,
                    label = { it.label },
                    isSelected = { it == state.background },
                    onSelect = state.onBackgroundChange,
                )
            }
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemMessageBubbleStory) {
    val background = when (state.background) {
        Background.BgPrimary -> TangemTheme.colors3.bg.primary
        Background.BgSecondary -> TangemTheme.colors3.bg.secondary
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, TangemTheme.colors3.border.tertiary, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        PreviewBubble(state = state)
    }
}

@Composable
private fun PreviewBubble(state: TangemMessageBubbleStory) {
    val isOneLine = state.message == TangemMessageBubbleStory.Message.OneLine
    TangemMessageBubble(
        text = stringReference(if (state.message == TangemMessageBubbleStory.Message.Short) SHORT_TEXT else LONG_TEXT),
        variant = state.variant,
        showTip = state.showTip,
        icon = if (state.hasIcon) Icons.ic_info_16 else null,
        onClick = if (state.isClickable) {
            {}
        } else {
            null
        },
        onClose = if (state.trailingButton == TrailingButton.None) {
            null
        } else {
            {}
        },
        trailingIcon = if (state.trailingButton == TrailingButton.Chevron) {
            Icons.ic_chevron_right_16
        } else {
            Icons.ic_cross_circle_16_filled
        },
        closeContentDescription = "Dismiss",
        maxLines = if (isOneLine) 1 else Int.MAX_VALUE,
        overflow = if (isOneLine) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

@Composable
private fun Toggles(state: TangemMessageBubbleStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "showTip", checked = state.showTip, onToggle = state.onTipToggle)
            ToggleRow(label = "icon", checked = state.hasIcon, onToggle = state.onIconToggle)
            ToggleRow(label = "clickable", checked = state.isClickable, onToggle = state.onClickableToggle)
        }
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = label,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors3.text.primary,
        )
        content()
    }
}

@Composable
private fun <T> ChipGrid(items: List<T>, label: (T) -> String, isSelected: (T) -> Boolean, onSelect: (T) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(TangemTheme.colors3.bg.primary)
            .border(
                width = 1.dp,
                color = TangemTheme.colors3.border.tertiary,
                shape = shape,
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            Chip(
                label = label(item),
                selected = isSelected(item),
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(
                if (selected) TangemTheme.colors3.bg.secondary else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TangemTheme.colors3.bg.primary)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors3.text.accent.blue else TangemTheme.colors3.text.secondary,
        )
    }
}