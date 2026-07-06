@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.messagebanner

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.messagebanner.CloseButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBannerStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageBannerStory.Background

@Composable
internal fun TangemMessageBannerStory(state: TangemMessageBannerStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
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
            VariantSelector(selected = state.variant, onSelect = state.onVariantChange)
            ContentAlignSelector(selected = state.contentAlign, onSelect = state.onContentAlignChange)
            BackgroundSelector(selected = state.background, onSelect = state.onBackgroundChange)
            Toggles(state = state)
        }
    }
}

@Composable
private fun PreviewBackground(background: Background, modifier: Modifier = Modifier) {
    when (background) {
        Background.BgPrimary -> Box(modifier.background(TangemTheme.colors3.bg.primary))
        Background.BgSecondary -> Box(modifier.background(TangemTheme.colors3.bg.secondary))
        Background.BgInverse -> Box(modifier.background(TangemTheme.colors3.bg.inverse))
    }
}

@Composable
private fun ComponentPreview(state: TangemMessageBannerStory) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        PreviewBackground(background = state.background, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            PreviewBanner(state = state)
        }
    }
}

@Composable
private fun PreviewBanner(state: TangemMessageBannerStory) {
    TangemMessageBanner(
        modifier = Modifier.fillMaxWidth(),
        variant = state.variant,
        contentAlign = state.contentAlign,
        showGlowRing = state.hasGlowRing,
        title = stringReference("Would you predict?"),
        description = if (state.hasDescription) {
            stringReference("France will win FIFA 2026")
        } else {
            null
        },
        secondaryButton = if (state.hasSecondaryButton) {
            TangemMessageBanner.Button(text = stringReference("Yes"), onClick = {})
        } else {
            null
        },
        primaryButton = if (state.hasPrimaryButton) {
            TangemMessageBanner.Button(text = stringReference("Oh, yes"), onClick = {})
        } else {
            null
        },
        slotStart = if (state.hasSlotStart) {
            { BannerLeadingIcon() }
        } else {
            null
        },
        slotEnd = when {
            state.hasCloseButton -> {
                { TangemMessageBanner.CloseButton(onClick = {}, contentDescription = "Dismiss") }
            }
            state.hasSlotEnd -> {
                { CirclePlaceholder(size = 24.dp) }
            }
            else -> null
        },
        extraBottomSlot = if (state.hasExtraContent) {
            { ProtectedByRow() }
        } else {
            null
        },
    )
}

@Composable
private fun BannerLeadingIcon() {
    Image(
        painter = painterResource(R.drawable.img_solana_22),
        contentDescription = null,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(percent = 50)),
    )
}

@Composable
private fun ProtectedByRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Protected by Tangem Security",
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Icon(
            painter = painterResource(R.drawable.ic_shield_check_16),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.status.info,
        )
    }
}

@Composable
private fun CirclePlaceholder(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 50))
            .background(TangemTheme.colors3.bg.tertiary),
    )
}

@Composable
private fun BackgroundSelector(selected: Background, onSelect: (Background) -> Unit) {
    Section(label = "Background") {
        ChipGrid(
            items = Background.entries,
            label = { it.label },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun VariantSelector(selected: TangemMessageBanner.Variant, onSelect: (TangemMessageBanner.Variant) -> Unit) {
    Section(label = "Variant") {
        ChipGrid(
            items = TangemMessageBanner.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun ContentAlignSelector(
    selected: TangemMessageBanner.ContentAlign,
    onSelect: (TangemMessageBanner.ContentAlign) -> Unit,
) {
    Section(label = "Content align") {
        ChipGrid(
            items = TangemMessageBanner.ContentAlign.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun Toggles(state: TangemMessageBannerStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "glowRing", checked = state.hasGlowRing, onToggle = state.onGlowRingToggle)
            ToggleRow(label = "description", checked = state.hasDescription, onToggle = state.onDescriptionToggle)
            ToggleRow(
                label = "secondaryButton",
                checked = state.hasSecondaryButton,
                onToggle = state.onSecondaryButtonToggle,
            )
            ToggleRow(label = "primaryButton", checked = state.hasPrimaryButton, onToggle = state.onPrimaryButtonToggle)
            ToggleRow(label = "closeButton", checked = state.hasCloseButton, onToggle = state.onCloseButtonToggle)
            ToggleRow(label = "slotStart", checked = state.hasSlotStart, onToggle = state.onSlotStartToggle)
            ToggleRow(label = "slotEnd", checked = state.hasSlotEnd, onToggle = state.onSlotEndToggle)
            ToggleRow(label = "extraContent", checked = state.hasExtraContent, onToggle = state.onExtraContentToggle)
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
                if (selected) TangemTheme.colors2.surface.level3 else Color.Transparent,
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