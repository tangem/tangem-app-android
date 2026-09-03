package com.tangem.core.ui.ds2.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 button group — a slot container that lays out one or more [TangemButton]s with
 * the design-system spacing and equalizes their size, so the buttons themselves are passed as
 * plain content without any sizing modifier.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=9202-4539)
 *
 * @param modifier Modifier applied to the group. Supply a width — see behavior notes.
 * @param orientation Layout direction of the buttons. See [TangemButtonGroup.Orientation].
 * @param content Buttons to lay out, in display order. Expected to emit [TangemButton]s.
 */
@Composable
fun TangemButtonGroup(
    modifier: Modifier = Modifier,
    orientation: TangemButtonGroup.Orientation = TangemButtonGroup.Orientation.Vertical,
    content: @Composable () -> Unit,
) {
    val spacing = with(LocalDensity.current) { 8.dp.roundToPx() }

    Layout(modifier = modifier, content = content) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(width = constraints.minWidth, height = constraints.minHeight) {}
        } else {
            when (orientation) {
                TangemButtonGroup.Orientation.Vertical -> measureVertically(measurables, constraints, spacing)
                TangemButtonGroup.Orientation.Horizontal -> measureHorizontally(measurables, constraints, spacing)
            }
        }
    }
}

private fun MeasureScope.measureVertically(
    measurables: List<Measurable>,
    constraints: Constraints,
    spacing: Int,
): MeasureResult {
    val childConstraints = if (constraints.hasBoundedWidth) {
        Constraints.fixedWidth(constraints.maxWidth)
    } else {
        Constraints()
    }
    val placeables = measurables.map { it.measure(childConstraints) }

    val width = placeables.maxOf { it.width }.coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = (placeables.sumOf { it.height } + spacing * placeables.lastIndex)
        .coerceIn(constraints.minHeight, constraints.maxHeight)

    return layout(width = width, height = height) {
        var y = 0
        placeables.forEach { placeable ->
            placeable.placeRelative(x = 0, y = y)
            y += placeable.height + spacing
        }
    }
}

private fun MeasureScope.measureHorizontally(
    measurables: List<Measurable>,
    constraints: Constraints,
    spacing: Int,
): MeasureResult {
    val totalSpacing = spacing * measurables.lastIndex
    val placeables = if (constraints.hasBoundedWidth) {
        val available = (constraints.maxWidth - totalSpacing).coerceAtLeast(minimumValue = 0)
        val equalWidth = available / measurables.size
        // The remainder of the integer division is handed out pixel by pixel, so the row fills the
        // whole width instead of leaving a sliver at the end.
        val extraPixels = available % measurables.size
        measurables.mapIndexed { index, measurable ->
            val childWidth = equalWidth + if (index < extraPixels) 1 else 0
            measurable.measure(Constraints.fixedWidth(childWidth).copy(maxHeight = constraints.maxHeight))
        }
    } else {
        measurables.map { it.measure(Constraints(maxHeight = constraints.maxHeight)) }
    }

    val width = (placeables.sumOf { it.width } + totalSpacing)
        .coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = placeables.maxOf { it.height }.coerceIn(constraints.minHeight, constraints.maxHeight)

    return layout(width = width, height = height) {
        var x = 0
        placeables.forEach { placeable ->
            placeable.placeRelative(x = x, y = (height - placeable.height) / 2)
            x += placeable.width + spacing
        }
    }
}

object TangemButtonGroup {

    /**
     * Layout direction of the buttons.
     *
     * - [Vertical] — buttons stacked top to bottom, each stretched to the group's width.
     * - [Horizontal] — buttons in a single row sharing the group's width equally.
     */
    enum class Orientation {
        Vertical,
        Horizontal,
    }
}

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemButtonGroupPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 20.dp),
        ) {
            TangemButtonGroup.Orientation.entries.forEach { orientation ->
                PreviewSection(label = orientation.name) {
                    TangemButtonGroup(modifier = Modifier.fillMaxWidth(), orientation = orientation) {
                        TangemButton(
                            variant = TangemButton.Variant.Secondary,
                            size = TangemButton.Size.X12,
                            text = stringReference(value = "Label"),
                            onClick = {},
                        )
                        TangemButton(
                            variant = TangemButton.Variant.Primary,
                            size = TangemButton.Size.X12,
                            text = stringReference(value = "Label"),
                            onClick = {},
                        )
                    }
                }
            }
            PreviewSection(label = "Single button") {
                TangemButtonGroup(modifier = Modifier.fillMaxWidth()) {
                    TangemButton(
                        variant = TangemButton.Variant.Primary,
                        size = TangemButton.Size.X12,
                        text = stringReference(value = "Label"),
                        onClick = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(
            text = label,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        content()
    }
}

// endregion