@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.avatar

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
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_grid_plus_20
import com.tangem.feature.tester.presentation.storybook.entity.TangemAvatarStory

private const val SAMPLE_ADDRESS = "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984"
private const val SAMPLE_INITIAL = 'A'
private const val FILL_COLORS_PER_ROW = 4

@Composable
internal fun TangemAvatarStory(state: TangemAvatarStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VariantSelector(selected = state.variant, onSelect = state.onVariantChange)
            when (state.variant) {
                TangemAvatarStory.Variant.Icon -> AppearanceSelector(
                    selected = state.appearance,
                    onSelect = state.onAppearanceChange,
                )
                TangemAvatarStory.Variant.Transaction -> StatusSelector(
                    selected = state.status,
                    onSelect = state.onStatusChange,
                )
                TangemAvatarStory.Variant.Account,
                TangemAvatarStory.Variant.User,
                -> FillColorSelector(selected = state.fillColor, onSelect = state.onFillColorChange)
                TangemAvatarStory.Variant.Address,
                TangemAvatarStory.Variant.Shimmer,
                -> Unit
            }
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemAvatarStory) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.primary)
            .padding(16.dp),
    ) {
        TangemAvatar.Size.entries.forEach { size ->
            TangemAvatar(state = state.toUiState(), size = size)
        }
    }
}

private fun TangemAvatarStory.toUiState(): TangemAvatar.UiState = when (variant) {
    TangemAvatarStory.Variant.Account -> TangemAvatar.UiState.Account(
        iconRes = R.drawable.ic_airplane_mode_24,
        color = fillColor,
    )
    TangemAvatarStory.Variant.User -> TangemAvatar.UiState.User(
        initial = SAMPLE_INITIAL.takeIf { hasUserInitial },
        color = fillColor,
    )
    TangemAvatarStory.Variant.Address -> TangemAvatar.UiState.Address(address = SAMPLE_ADDRESS)
    TangemAvatarStory.Variant.Icon -> TangemAvatar.UiState.Icon(
        imageVector = Icons.ic_grid_plus_20,
        appearance = appearance,
    )
    TangemAvatarStory.Variant.Transaction -> TangemAvatar.UiState.Transaction(
        imageVector = Icons.ic_arrow_up_20,
        status = status,
        hasPendingIndicator = hasPendingIndicator,
    )
    TangemAvatarStory.Variant.Shimmer -> TangemAvatar.UiState.Shimmer(
        shape = if (isShimmerRounded) {
            TangemAvatar.UiState.Shimmer.Shape.RoundedSquare
        } else {
            TangemAvatar.UiState.Shimmer.Shape.Circle
        },
    )
}

@Composable
private fun VariantSelector(selected: TangemAvatarStory.Variant, onSelect: (TangemAvatarStory.Variant) -> Unit) {
    Section(label = "Variant") {
        ChipGrid(
            items = TangemAvatarStory.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun AppearanceSelector(selected: TangemAvatar.Appearance, onSelect: (TangemAvatar.Appearance) -> Unit) {
    Section(label = "Appearance") {
        ChipGrid(
            items = TangemAvatar.Appearance.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun StatusSelector(selected: TangemAvatar.Status, onSelect: (TangemAvatar.Status) -> Unit) {
    Section(label = "Status") {
        ChipGrid(
            items = TangemAvatar.Status.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun FillColorSelector(selected: TangemAvatar.FillColor, onSelect: (TangemAvatar.FillColor) -> Unit) {
    Section(label = "Fill color") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TangemAvatar.FillColor.entries.chunked(FILL_COLORS_PER_ROW).forEach { row ->
                ChipGrid(
                    items = row,
                    label = { it.name },
                    isSelected = { it == selected },
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun Toggles(state: TangemAvatarStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(
                label = "pendingIndicator",
                checked = state.hasPendingIndicator,
                onToggle = state.onPendingIndicatorToggle,
            )
            ToggleRow(label = "userInitial", checked = state.hasUserInitial, onToggle = state.onUserInitialToggle)
            ToggleRow(
                label = "shimmerRoundedSquare",
                checked = state.isShimmerRounded,
                onToggle = state.onShimmerRoundedToggle,
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