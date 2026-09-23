@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.statusscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemStatusScreenStory

@Composable
internal fun TangemStatusScreenStory(state: TangemStatusScreenStory, modifier: Modifier = Modifier) {
    if (state.isFullscreen) {
        BackHandler(onBack = state.onFullscreenToggle)
        Box(modifier = modifier.fillMaxSize()) {
            key(state.replayKey) {
                StatusScreen(state = state)
            }
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
            .systemBarsPadding()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VariantSelector(selected = state.variant, onSelect = state.onVariantChange)
            SlotToggles(state = state)
            ReplayRow(onClick = state.onReplayClick)
            FullscreenRow(onClick = state.onFullscreenToggle)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemStatusScreenStory) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(600.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        key(state.replayKey) {
            StatusScreen(state = state)
        }
    }
}

@Composable
private fun StatusScreen(state: TangemStatusScreenStory) {
    TangemStatusScreen(
        variant = state.variant,
        title = stringReference(value = "Prediction placed"),
        description = if (state.hasDescription) {
            stringReference(value = "Aug 12, 13:24. We are not sorry for that.")
        } else {
            null
        },
        asset = if (state.hasAsset) {
            { PreviewAsset(variant = state.variant) }
        } else {
            null
        },
        extraBottom = if (state.hasExtraBottom) {
            {
                TangemBadge(
                    text = stringReference(value = "Current 5.5% net APY"),
                    status = TangemBadge.Status.Success,
                    size = TangemBadge.Size.X6,
                )
            }
        } else {
            null
        },
        details = if (state.hasDetails) {
            {
                DetailsRow(title = "Tokens bought", value = "500", divider = true)
                DetailsRow(title = "Resolution Date", value = "22.03.2026", divider = true)
                DetailsRow(title = "Fee", value = "$0.02", divider = false)
            }
        } else {
            null
        },
        footer = if (state.hasFooter) {
            {
                TangemButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = stringReference(value = "To main"),
                    onClick = {},
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun PreviewAsset(variant: TangemStatusScreen.Variant) {
    val kind = when (variant) {
        TangemStatusScreen.Variant.Success -> TangemStatusIcon.Kind.Checkmark
        TangemStatusScreen.Variant.Error -> TangemStatusIcon.Kind.HeartBroken
        TangemStatusScreen.Variant.Warning -> TangemStatusIcon.Kind.Warning
        TangemStatusScreen.Variant.Neutral,
        TangemStatusScreen.Variant.Info,
        -> TangemStatusIcon.Kind.Info
    }
    if (variant == TangemStatusScreen.Variant.Neutral) {
        TangemStatusIcon(kind = kind, modifier = Modifier.size(40.dp), tint = TangemTheme.colors3.icon.secondary)
    } else {
        TangemStatusIcon(kind = kind, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun DetailsRow(title: String, value: String, divider: Boolean) {
    TangemRow(
        divider = divider,
        titleSlot = {
            Text(
                text = title,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        },
        valueSlot = {
            Text(
                text = value,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
        },
    )
}

@Composable
private fun VariantSelector(selected: TangemStatusScreen.Variant, onSelect: (TangemStatusScreen.Variant) -> Unit) {
    Section(label = "Variant") {
        ChipGrid(
            items = TangemStatusScreen.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun SlotToggles(state: TangemStatusScreenStory) {
    Section(label = "Slots") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "description", checked = state.hasDescription, onToggle = state.onDescriptionToggle)
            ToggleRow(label = "asset", checked = state.hasAsset, onToggle = state.onAssetToggle)
            ToggleRow(
                label = "extra bottom (badge)",
                checked = state.hasExtraBottom,
                onToggle = state.onExtraBottomToggle,
            )
            ToggleRow(label = "details", checked = state.hasDetails, onToggle = state.onDetailsToggle)
            ToggleRow(label = "footer", checked = state.hasFooter, onToggle = state.onFooterToggle)
        }
    }
}

@Composable
private fun ReplayRow(onClick: () -> Unit) {
    Section(label = "Animation") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TangemTheme.colors2.surface.level2)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Replay entrance",
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = "▶",
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.accent,
            )
        }
    }
}

@Composable
private fun FullscreenRow(onClick: () -> Unit) {
    Section(label = "Fullscreen") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TangemTheme.colors2.surface.level2)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Open fullscreen",
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = "back to exit",
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.secondary,
            )
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
            color = TangemTheme.colors.text.primary1,
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
            .background(TangemTheme.colors2.surface.level2)
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.secondary,
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
                if (selected) TangemTheme.colors2.surface.level3 else TangemTheme.colors2.surface.level2,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TangemTheme.colors2.surface.level2)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors.text.accent else TangemTheme.colors.text.secondary,
        )
    }
}