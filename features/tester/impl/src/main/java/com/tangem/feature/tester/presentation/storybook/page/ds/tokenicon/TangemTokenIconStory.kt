@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.tokenicon

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemTokenIconStory

private const val SAMPLE_URL = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/bitcoin.png"

@Composable
internal fun TangemTokenIconStory(state: TangemTokenIconStory, modifier: Modifier = Modifier) {
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
            UiStateSelector(selected = state.uiState, onSelect = state.onUiStateChange)
            SizeSelector(selected = state.size, onSelect = state.onSizeChange)
            // The State flags only apply to the Token variant of the UiState overload.
            if (state.uiState == TangemTokenIconStory.UiStateVariant.Token) {
                Toggles(state = state)
            }
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemTokenIconStory) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(vertical = 48.dp),
    ) {
        // Rendered through the high-level UiState overload.
        val uiState = when (state.uiState) {
            TangemTokenIconStory.UiStateVariant.Token -> TangemTokenIcon.UiState.Token(
                tokenState = TangemTokenIcon.State(
                    url = if (state.hasUrl) SAMPLE_URL else null,
                    topIcon = if (state.hasTopIcon) ImageVector.vectorResource(R.drawable.img_bsc_22) else null,
                    isGrayscale = state.isGrayscale,
                    indicator = when {
                        !state.hasIndicator -> null
                        state.isIndicatorPurple -> TangemTokenIcon.State.Indicator(
                            colorReference2 = ColorReference2 { TangemTheme.colors3.icon.accent.violet },
                        )
                        else -> TangemTokenIcon.State.Indicator()
                    },
                ),
            )
            TangemTokenIconStory.UiStateVariant.Shimmer -> TangemTokenIcon.UiState.Shimmer
            TangemTokenIconStory.UiStateVariant.Error -> TangemTokenIcon.UiState.Error
        }
        TangemTokenIcon(state = uiState, size = state.size)
    }
}

@Composable
private fun UiStateSelector(
    selected: TangemTokenIconStory.UiStateVariant,
    onSelect: (TangemTokenIconStory.UiStateVariant) -> Unit,
) {
    Section(label = "UiState") {
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
            TangemTokenIconStory.UiStateVariant.entries.forEach { variant ->
                Chip(
                    label = variant.name,
                    selected = variant == selected,
                    onClick = { onSelect(variant) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SizeSelector(selected: TangemTokenIcon.Size, onSelect: (TangemTokenIcon.Size) -> Unit) {
    Section(label = "Size") {
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
            TangemTokenIcon.Size.entries.forEach { size ->
                Chip(
                    label = size.name,
                    selected = size == selected,
                    onClick = { onSelect(size) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Toggles(state: TangemTokenIconStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "hasUrl (else fallback)", checked = state.hasUrl, onToggle = state.onUrlToggle)
            ToggleRow(label = "isGrayscale", checked = state.isGrayscale, onToggle = state.onGrayscaleToggle)
            ToggleRow(label = "hasIndicator", checked = state.hasIndicator, onToggle = state.onIndicatorToggle)
            ToggleRow(
                label = "indicator purple (else default)",
                checked = state.isIndicatorPurple,
                onToggle = state.onIndicatorPurpleToggle,
            )
            ToggleRow(label = "hasTopIcon", checked = state.hasTopIcon, onToggle = state.onTopIconToggle)
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