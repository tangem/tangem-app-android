@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.buttongroup

import androidx.compose.foundation.background
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
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemButtonGroupStory

private const val MAX_BUTTON_COUNT = 3

private val PreviewVariants = listOf(
    TangemButton.Variant.Outline,
    TangemButton.Variant.Secondary,
    TangemButton.Variant.Primary,
)

private val PreviewLabels = listOf("Learn more", "Cancel", "Confirm")

private val PreviewLongLabels = listOf(
    "Learn more about staking",
    "Cancel this operation",
    "Confirm and send payment",
)

@Composable
internal fun TangemButtonGroupStory(state: TangemButtonGroupStory, modifier: Modifier = Modifier) {
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
            OrientationSelector(selected = state.orientation, onSelect = state.onOrientationChange)
            ButtonCountSelector(selected = state.buttonCount, onSelect = state.onButtonCountChange)
            SizeSelector(selected = state.size, onSelect = state.onSizeChange)
            Toggles(state = state)
            Hint()
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemButtonGroupStory) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(all = 16.dp),
    ) {
        val icon = if (state.hasIcon) TangemIconUM.Icon(iconRes = R.drawable.ic_information_24) else null
        val labels = if (state.isLongLabel) PreviewLongLabels else PreviewLabels

        TangemButtonGroup(
            modifier = if (state.isFillWidth) Modifier.fillMaxWidth() else Modifier,
            orientation = state.orientation,
        ) {
            PreviewVariants.takeLast(state.buttonCount)
                .zip(labels.takeLast(state.buttonCount))
                .forEach { (variant, label) ->
                    TangemButton(
                        variant = variant,
                        size = state.size,
                        text = stringReference(label),
                        iconStart = icon,
                        onClick = {},
                    )
                }
        }
    }
}

@Composable
private fun OrientationSelector(
    selected: TangemButtonGroup.Orientation,
    onSelect: (TangemButtonGroup.Orientation) -> Unit,
) {
    Section(label = "Orientation") {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TangemButtonGroup.Orientation.entries.forEach { orientation ->
                Chip(
                    label = orientation.name,
                    selected = orientation == selected,
                    onClick = { onSelect(orientation) },
                )
            }
        }
    }
}

@Composable
private fun ButtonCountSelector(selected: Int, onSelect: (Int) -> Unit) {
    Section(label = "Buttons") {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (count in 1..MAX_BUTTON_COUNT) {
                Chip(
                    label = count.toString(),
                    selected = count == selected,
                    onClick = { onSelect(count) },
                )
            }
        }
    }
}

@Composable
private fun SizeSelector(selected: TangemButton.Size, onSelect: (TangemButton.Size) -> Unit) {
    Section(label = "Button size") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TangemButton.Size.entries.forEach { size ->
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
private fun Toggles(state: TangemButtonGroupStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "fill max width", checked = state.isFillWidth, onToggle = state.onFillWidthToggle)
            ToggleRow(label = "long labels", checked = state.isLongLabel, onToggle = state.onLongLabelToggle)
            ToggleRow(label = "icon", checked = state.hasIcon, onToggle = state.onIconToggle)
        }
    }
}

@Composable
private fun Hint() {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = "The group sizes the buttons itself: stretched to its width when vertical, split " +
            "equally when horizontal. Turn \"fill max width\" off to see the group hug its content, " +
            "where every button keeps its own width.",
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
            .padding(vertical = 8.dp, horizontal = 12.dp),
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