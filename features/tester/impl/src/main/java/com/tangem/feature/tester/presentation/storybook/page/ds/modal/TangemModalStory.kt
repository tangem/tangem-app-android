@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.modal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.modal.LocalTangemModalHost
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TangemModalHost
import com.tangem.core.ui.ds2.modal.rememberTangemModalHostState
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemModalStory
import com.tangem.feature.tester.presentation.storybook.page.ds.TransparentSystemBarsEffect
import dev.chrisbanes.haze.rememberHazeState

@Composable
internal fun TangemModalStory(state: TangemModalStory, modifier: Modifier = Modifier) {
    TransparentSystemBarsEffect()

    // The page hosts its own same-window modal host, so the overlay presentation is demoable
    // in the tester (whose activity has no root host) — including the real backdrop blur.
    val modalHostState = rememberTangemModalHostState()
    val modalHazeState = rememberHazeState()

    CompositionLocalProvider(LocalTangemModalHost provides modalHostState) {
        Box(modifier = modifier.fillMaxSize()) {
            Controls(
                state = state,
                modifier = Modifier
                    .matchParentSize()
                    .hazeSourceTangem(state = modalHazeState),
            )
            TangemModalHost(
                state = modalHostState,
                hazeState = modalHazeState,
                modifier = Modifier.matchParentSize(),
            )
        }

        StoryModal(state = state)
        StackedStoryModal(state = state)
    }
}

@Composable
private fun StoryModal(state: TangemModalStory) {
    TangemModal<TangemModalStoryContent>(
        config = TangemBottomSheetConfig(
            isShown = state.isShown,
            onDismissRequest = { state.onShownChange(false) },
            content = TangemModalStoryContent,
        ),
        isExpanded = state.isExpanded,
        scrollableContent = state.isContentScrollable,
        presentation = state.presentation,
        title = {
            TangemTopNavigation(
                title = stringReference("Title"),
                subtitle = stringReference("Subtitle"),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0.dp),
                fadeEnabled = false,
                onBack = {},
                onClose = { state.onShownChange(false) },
            )
        },
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            ShowModalButton(label = "Show stacked modal", onClick = { state.onStackedShownChange(true) })
        }
        val rowCount = if (state.isTallContent) 40 else 4
        repeat(times = rowCount) { index ->
            Text(
                text = "Row ${index + 1}",
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun StackedStoryModal(state: TangemModalStory) {
    TangemModal<TangemModalStoryContent>(
        config = TangemBottomSheetConfig(
            isShown = state.isStackedShown,
            onDismissRequest = { state.onStackedShownChange(false) },
            content = TangemModalStoryContent,
        ),
        presentation = state.presentation,
        title = {
            TangemTopNavigation(
                title = stringReference("Stacked"),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0.dp),
                fadeEnabled = false,
                onBack = null,
                onClose = { state.onStackedShownChange(false) },
            )
        },
    ) {
        repeat(times = 3) { index ->
            Text(
                text = "Stacked row ${index + 1}",
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun Controls(state: TangemModalStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors2.surface.level1)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShowModalButton(label = "Show modal", onClick = { state.onShownChange(true) })
        PresentationSelector(selected = state.presentation, onSelect = state.onPresentationChange)
        ToggleRow(label = "isExpanded", checked = state.isExpanded, onToggle = state.onExpandedToggle)
        ToggleRow(
            label = "scrollable content",
            checked = state.isContentScrollable,
            onToggle = state.onContentScrollableToggle,
        )
        ToggleRow(label = "tall content", checked = state.isTallContent, onToggle = state.onTallContentToggle)
    }
}

@Composable
private fun PresentationSelector(selected: TangemModal.Presentation, onSelect: (TangemModal.Presentation) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TangemTheme.colors2.surface.level2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TangemModal.Presentation.entries.forEach { presentation ->
            Chip(
                label = presentation.name,
                selected = presentation == selected,
                onClick = { onSelect(presentation) },
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

private object TangemModalStoryContent : TangemBottomSheetConfigContent

@Composable
private fun ShowModalButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TangemTheme.colors2.surface.level3)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
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