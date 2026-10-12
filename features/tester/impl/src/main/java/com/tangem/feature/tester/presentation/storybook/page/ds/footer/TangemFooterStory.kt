@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.footer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_left_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_right_20
import com.tangem.feature.tester.presentation.storybook.entity.TangemFooterStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemFooterStory.Actions

private const val HINT_TEXT = "Within 3 business days you will receive an email from logistic company " +
    "on user@example.com"

@Composable
internal fun TangemFooterStory(state: TangemFooterStory, modifier: Modifier = Modifier) {
    val hazeState = LocalHazeState.current
    SideEffect { hazeState.blurEnabled = state.isBlurEnabled }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ActionsSelector(selected = state.actions, onSelect = state.onActionsChange)
            FadeVariantSelector(selected = state.fadeVariant, onSelect = state.onFadeVariantChange)
            Toggles(state = state)
            Hint()
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemFooterStory, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        ScrollingContent(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(zIndex = -1f),
        )
        Footer(
            state = state,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding(),
        )
    }
}

/** Content the footer is pinned over, so the fade and its blur have something to cut off. */
@Composable
private fun ScrollingContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.background(TangemTheme.colors3.bg.primary),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(count = 20) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (index % 2 == 0) {
                            TangemTheme.colors3.bg.accent.blue
                        } else {
                            TangemTheme.colors3.bg.accent.violet
                        },
                    ),
            )
        }
    }
}

@Composable
private fun Footer(state: TangemFooterStory, modifier: Modifier = Modifier) {
    val windowInsets = if (state.isInModal) TangemFooter.ModalWindowInsets else WindowInsets.navigationBars
    val extraTop: (@Composable () -> Unit)? = if (state.hasExtraTop) {
        { ExtraTopHint() }
    } else {
        null
    }

    if (state.actions == Actions.StartEnd) {
        TangemFooter(
            modifier = modifier,
            windowInsets = windowInsets,
            fadeEnabled = state.isFadeEnabled,
            fadeVariant = state.fadeVariant,
            blurBackground = state.isBlurEnabled,
            extraTop = extraTop,
            buttonStart = {
                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X11,
                    iconStart = TangemIconUM.Icon(Icons.ic_arrow_left_20),
                    onClick = {},
                )
            },
            buttonEnd = {
                TangemButton(
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X11,
                    iconStart = TangemIconUM.Icon(Icons.ic_arrow_right_20),
                    onClick = {},
                )
            },
        )
    } else {
        TangemFooter(
            modifier = modifier,
            windowInsets = windowInsets,
            fadeEnabled = state.isFadeEnabled,
            fadeVariant = state.fadeVariant,
            blurBackground = state.isBlurEnabled,
            extraTop = extraTop,
        ) {
            ActionsContent(actions = state.actions)
        }
    }
}

@Composable
private fun ActionsContent(actions: Actions) {
    when (actions) {
        Actions.ButtonGroupVertical -> TangemButtonGroup {
            SecondaryButton()
            PrimaryButton()
        }
        Actions.ButtonGroupHorizontal -> TangemButtonGroup(
            orientation = TangemButtonGroup.Orientation.Horizontal,
        ) {
            SecondaryButton()
            PrimaryButton()
        }
        Actions.SingleButton -> TangemButtonGroup { PrimaryButton() }
        Actions.Search -> SearchField()
        Actions.StartEnd -> Unit // handled by the start/end overload in Footer
    }
}

@Composable
private fun SecondaryButton() {
    TangemButton(
        variant = TangemButton.Variant.Secondary,
        size = TangemButton.Size.X12,
        text = stringReference("Cancel"),
        onClick = {},
    )
}

@Composable
private fun PrimaryButton() {
    TangemButton(
        variant = TangemButton.Variant.Primary,
        size = TangemButton.Size.X12,
        text = stringReference("Confirm"),
        onClick = {},
    )
}

@Composable
private fun SearchField() {
    var query by rememberSaveable { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }
    TangemSearch(
        state = TangemSearch.State(
            placeholderText = stringReference("Search"),
            query = query,
            onQueryChange = { query = it },
            isActive = isActive,
            onActiveChange = { isActive = it },
            onClearClick = { query = "" },
        ),
    )
}

@Composable
private fun ExtraTopHint() {
    Text(
        text = HINT_TEXT,
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ActionsSelector(selected: Actions, onSelect: (Actions) -> Unit) {
    Section(label = "Actions") {
        ChipGrid(
            items = Actions.entries,
            label = { it.label },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun FadeVariantSelector(selected: TangemFade.Variant, onSelect: (TangemFade.Variant) -> Unit) {
    Section(label = "Fade") {
        ChipGrid(
            items = TangemFade.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun Toggles(state: TangemFooterStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "extra top", checked = state.hasExtraTop, onToggle = state.onExtraTopToggle)
            ToggleRow(label = "fade", checked = state.isFadeEnabled, onToggle = state.onFadeToggle)
            ToggleRow(label = "blur", checked = state.isBlurEnabled, onToggle = state.onBlurToggle)
            ToggleRow(label = "in modal (24dp bottom)", checked = state.isInModal, onToggle = state.onInModalToggle)
        }
    }
}

@Composable
private fun Hint() {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = "Pick \"search\" and tap the field: with the keyboard open the footer's vertical " +
            "padding grows to 12dp and the bottom inset is dropped. \"in modal\" swaps the safe-area " +
            "inset for the fixed 24dp home-indicator padding. The \"Cancel\" button is the Material " +
            "variant: it must blur the fade above the list, not the list itself.",
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.secondary,
    )
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
            maxLines = 1,
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