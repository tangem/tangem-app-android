@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.toast

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.toast.TangemToast
import com.tangem.core.ui.ds2.toast.TangemToastStack
import com.tangem.core.ui.ds2.toast.TangemToastStackState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.TangemToastMessage
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_eye_cross_20
import com.tangem.feature.tester.presentation.storybook.entity.TangemToastStory
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun TangemToastStory(state: TangemToastStory, modifier: Modifier = Modifier) {
    val toastStackState = remember { TangemToastStackState() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem()
                .background(TangemTheme.colors3.bg.primary)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ComponentPreview(state = state)
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Playback(
                    onShow = { scope.launch { toastStackState.show(state.toMessage()) } },
                    onSingle = { scope.playSingle(toastStackState) },
                    onMultiple = { scope.playMultiple(toastStackState) },
                    onMultipleLong = { scope.playMultipleLong(toastStackState) },
                )
                AppearanceSelector(selected = state.appearance, onSelect = state.onAppearanceChange)
                DurationSelector(selected = state.duration, onSelect = state.onDurationChange)
                Toggles(state = state)
            }
        }
        TangemToastStack(state = toastStackState)
    }
}

@Composable
private fun ComponentPreview(state: TangemToastStory) {
    val previewHazeState = rememberHazeState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeSourceTangem(state = previewHazeState)
                .background(TangemTheme.colors3.bg.secondary),
        )
        CompositionLocalProvider(LocalHazeState provides previewHazeState) {
            TangemToast(
                message = state.toMessage(),
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
            )
        }
    }
}

private fun TangemToastStory.toMessage() = TangemToastMessage(
    label = stringReference(if (isMultiLine) MULTI_LINE_LABEL else SHORT_LABEL),
    appearance = appearance,
    icon = Icons.ic_eye_cross_20.takeIf { hasIcon },
    duration = duration,
    actionLabel = stringReference("Unhide").takeIf { hasAction },
    action = {}.takeIf { hasAction },
    isClosable = isClosable,
)

private fun scenarioToast(label: String = SHORT_LABEL) = TangemToastMessage(
    label = stringReference(label),
    icon = Icons.ic_eye_cross_20,
    actionLabel = stringReference("Unhide"),
    action = {},
    isClosable = false,
)

private fun CoroutineScope.playSingle(state: TangemToastStackState) {
    launch { state.show(scenarioToast()) }
}

private fun CoroutineScope.playMultiple(state: TangemToastStackState) {
    launch {
        launch { state.show(scenarioToast()) }
        delay(500)
        launch { state.show(scenarioToast()) }
    }
}

private fun CoroutineScope.playMultipleLong(state: TangemToastStackState) {
    launch {
        launch { state.show(scenarioToast()) }
        delay(500)
        launch { state.show(scenarioToast()) }
        delay(500)
        launch { state.show(scenarioToast(label = MULTI_LINE_LABEL).copy(actionLabel = null, action = null)) }
    }
}

@Composable
private fun Playback(onShow: () -> Unit, onSingle: () -> Unit, onMultiple: () -> Unit, onMultipleLong: () -> Unit) {
    Section(label = "Show") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringReference("Show configured toast"),
                size = TangemButton.Size.X10,
                onClick = onShow,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScenarioButton(text = "Single", onClick = onSingle, modifier = Modifier.weight(1f))
                ScenarioButton(text = "Multiple", onClick = onMultiple, modifier = Modifier.weight(1f))
                ScenarioButton(text = "Multiple long", onClick = onMultipleLong, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScenarioButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier,
        variant = TangemButton.Variant.Secondary,
        size = TangemButton.Size.X9,
        text = stringReference(text),
        onClick = onClick,
    )
}

@Composable
private fun AppearanceSelector(selected: TangemToast.Appearance, onSelect: (TangemToast.Appearance) -> Unit) {
    Section(label = "Appearance") {
        ChipGrid(
            items = TangemToast.Appearance.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun DurationSelector(selected: TangemToast.Duration, onSelect: (TangemToast.Duration) -> Unit) {
    Section(label = "Duration") {
        ChipGrid(
            items = TangemToast.Duration.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun Toggles(state: TangemToastStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "multi-line label", checked = state.isMultiLine, onToggle = state.onMultiLineToggle)
            ToggleRow(label = "icon (Neutral)", checked = state.hasIcon, onToggle = state.onIconToggle)
            ToggleRow(label = "action", checked = state.hasAction, onToggle = state.onActionToggle)
            ToggleRow(label = "closable", checked = state.isClosable, onToggle = state.onClosableToggle)
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
            .border(width = 1.dp, color = TangemTheme.colors3.border.tertiary, shape = shape)
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) TangemTheme.colors3.bg.secondary else Color.Transparent)
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = TangemTheme.typography.body2, color = TangemTheme.colors3.text.primary)
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors3.text.accent.blue else TangemTheme.colors3.text.secondary,
        )
    }
}

private const val SHORT_LABEL = "Balance is currently hidden"
private const val MULTI_LINE_LABEL = "Balance is currently very very very hidden, tap Unhide to show it again"