@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.row

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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemRowStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemRowStory.Background

private const val SHORT_TITLE = "Title"
private const val LONG_TITLE = "Recipient address that should ellipsize when constrained by width"
private const val SUBTITLE = "Subtitle"
private const val VALUE = "0.0421 ETH"
private const val SUBVALUE = "≈ $124.80"

@Composable
internal fun TangemRowStory(state: TangemRowStory, modifier: Modifier = Modifier) {
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
            ContentLeadSelector(selected = state.contentLead, onSelect = state.onContentLeadChange)
            VerticalAlignmentSelector(
                selected = state.verticalAlignment,
                onSelect = state.onVerticalAlignmentChange,
            )
            BackgroundSelector(selected = state.background, onSelect = state.onBackgroundChange)
            TextScaleSlider(value = state.textScale, onChange = state.onTextScaleChange)
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemRowStory) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        PreviewBackground(background = state.background, modifier = Modifier.matchParentSize())
        val baseDensity = LocalDensity.current
        val scaledDensity = remember(baseDensity, state.textScale) {
            Density(density = baseDensity.density, fontScale = state.textScale)
        }
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            Box(modifier = Modifier.padding(vertical = 24.dp)) {
                StorybookRow(state = state)
            }
        }
    }
}

@Composable
private fun StorybookRow(state: TangemRowStory) {
    val titleText = if (state.longTitle) LONG_TITLE else SHORT_TITLE
    TangemRow(
        contentLead = state.contentLead,
        verticalAlignment = state.verticalAlignment,
        divider = state.divider,
        includeInnerPaddings = state.includeInnerPaddings,
        titleSlot = { TangemRowText(text = titleText, role = TangemRowTextRole.Title) },
        subtitleSlot = if (state.hasSubtitle) {
            { TangemRowText(text = SUBTITLE, role = TangemRowTextRole.Subtitle) }
        } else {
            null
        },
        valueSlot = if (state.hasValue) {
            { TangemRowText(text = VALUE, role = TangemRowTextRole.Value) }
        } else {
            null
        },
        subvalueSlot = if (state.hasSubvalue) {
            { TangemRowText(text = SUBVALUE, role = TangemRowTextRole.Subvalue) }
        } else {
            null
        },
        startSlot = if (state.hasStartSlot) {
            {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_information_24),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.primary,
                )
            }
        } else {
            null
        },
        endSlot = if (state.hasEndSlot) {
            {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_chevron_24),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.secondary,
                )
            }
        } else {
            null
        },
        extraBottomSlot = if (state.hasExtraBottom) {
            {
                Text(
                    text = "Extra bottom slot — multi-line description content.",
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.caption.medium,
                )
            }
        } else {
            null
        },
        onClick = if (state.isClickable) {
            { /* no-op — ripple + focus demo */ }
        } else {
            null
        },
    )
}

@Composable
private fun PreviewBackground(background: Background, modifier: Modifier = Modifier) {
    when (background) {
        Background.BgPrimary -> Box(modifier.background(TangemTheme.colors3.bg.primary))
        Background.BgSecondary -> Box(modifier.background(TangemTheme.colors3.bg.secondary))
        Background.BgBrand -> Box(modifier.background(TangemTheme.colors3.bg.brand))
        Background.BgInverse -> Box(modifier.background(TangemTheme.colors3.bg.inverse))
    }
}

@Composable
private fun ContentLeadSelector(selected: TangemRowContentLead, onSelect: (TangemRowContentLead) -> Unit) {
    Section(label = "Content lead") {
        ChipGrid(
            items = TangemRowContentLead.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun VerticalAlignmentSelector(
    selected: TangemRowVerticalAlignment,
    onSelect: (TangemRowVerticalAlignment) -> Unit,
) {
    Section(label = "Vertical alignment") {
        ChipGrid(
            items = TangemRowVerticalAlignment.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
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
private fun TextScaleSlider(value: Float, onChange: (Float) -> Unit) {
    Section(label = "Text scale: ${"%.2f".format(value)}x") {
        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = value,
            onValueChange = onChange,
            valueRange = 0.5f..2f,
            steps = 14,
            colors = SliderDefaults.colors(
                thumbColor = TangemTheme.colors.text.accent,
                activeTrackColor = TangemTheme.colors.text.accent,
                activeTickColor = TangemTheme.colors2.surface.level3,
                inactiveTrackColor = TangemTheme.colors2.surface.level3,
                inactiveTickColor = TangemTheme.colors.text.accent,
            ),
        )
    }
}

@Composable
private fun Toggles(state: TangemRowStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "divider", checked = state.divider, onToggle = state.onDividerToggle)
            ToggleRow(
                label = "includeInnerPaddings",
                checked = state.includeInnerPaddings,
                onToggle = state.onInnerPaddingsToggle,
            )
            ToggleRow(
                label = "clickable (ripple + focus)",
                checked = state.isClickable,
                onToggle = state.onClickableToggle,
            )
            ToggleRow(
                label = "startSlot (leading icon)",
                checked = state.hasStartSlot,
                onToggle = state.onStartSlotToggle,
            )
            ToggleRow(
                label = "endSlot (trailing icon)",
                checked = state.hasEndSlot,
                onToggle = state.onEndSlotToggle,
            )
            ToggleRow(label = "subtitle", checked = state.hasSubtitle, onToggle = state.onSubtitleToggle)
            ToggleRow(label = "value", checked = state.hasValue, onToggle = state.onValueToggle)
            ToggleRow(label = "subvalue", checked = state.hasSubvalue, onToggle = state.onSubvalueToggle)
            ToggleRow(
                label = "extraBottomSlot",
                checked = state.hasExtraBottom,
                onToggle = state.onExtraBottomToggle,
            )
            ToggleRow(
                label = "long title (ellipsis test)",
                checked = state.longTitle,
                onToggle = state.onLongTitleToggle,
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