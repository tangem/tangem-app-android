@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.shimmer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.RectangleShimmer
import com.tangem.core.ui.ds2.shimmers.TextShimmer
import com.tangem.core.ui.ds2.shimmers.TextShimmerStyle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemShimmerStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemShimmerStory.*

@Composable
internal fun TangemShimmerStory(state: TangemShimmerStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        ChipSection(label = "Text style") {
            ChipGrid(
                items = TextShimmerStyle.entries,
                label = { it.chipLabel() },
                isSelected = { it == state.textStyle },
                onSelect = state.onTextStyleChange,
            )
        }
        ChipSection(label = "Radius") {
            ChipGrid(
                items = RadiusOption.entries,
                label = { it.label },
                isSelected = { it == state.radius },
                onSelect = state.onRadiusChange,
            )
        }
        ChipSection(label = "Rectangle width") {
            ChipGrid(
                items = RectangleWidthOption.entries,
                label = { it.label },
                isSelected = { it == state.rectangleWidth },
                onSelect = state.onRectangleWidthChange,
            )
        }
        ChipSection(label = "Rectangle height") {
            ChipGrid(
                items = RectangleHeightOption.entries,
                label = { it.label },
                isSelected = { it == state.rectangleHeight },
                onSelect = state.onRectangleHeightChange,
            )
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemShimmerStory) {
    val radius = state.radius.value()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(TangemTheme.dimens3.borderRadius.b200))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(vertical = 24.dp, horizontal = 16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            PreviewLabel(text = "RectangleShimmer")
            RectangleShimmerPreview(
                width = state.rectangleWidth,
                height = state.rectangleHeight,
                radius = radius,
            )

            PreviewLabel(text = "TextShimmer · ${state.textStyle.chipLabel()}")
            TextShimmer(
                text = SAMPLE_TEXT,
                style = state.textStyle,
                radius = radius,
            )
        }
    }
}

@Composable
private fun RectangleShimmerPreview(width: RectangleWidthOption, height: RectangleHeightOption, radius: Dp) {
    val sizeModifier = when (width) {
        RectangleWidthOption.FILL -> Modifier.fillMaxWidth()
        else -> Modifier.width(width.value())
    }.height(height.value())

    RectangleShimmer(
        modifier = sizeModifier,
        radius = radius,
    )
}

@Composable
private fun PreviewLabel(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

// region Chip selector — uses ds2 surfaces/typography so the controls match the redesign.

@Composable
private fun ChipSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = label,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        content()
    }
}

@Composable
private fun <T> ChipGrid(items: List<T>, label: (T) -> String, isSelected: (T) -> Boolean, onSelect: (T) -> Unit) {
    val shape = RoundedCornerShape(TangemTheme.dimens3.borderRadius.full)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(TangemTheme.colors3.bg.opaque.primary)
            .border(
                width = TangemTheme.dimens3.borderWidth.sm,
                color = TangemTheme.colors3.border.primary,
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
    val chipShape = RoundedCornerShape(TangemTheme.dimens3.borderRadius.full)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(
                if (selected) TangemTheme.colors3.bg.opaque.secondary else TangemTheme.colors3.bg.opaque.primary,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography3.caption.medium,
            color = if (selected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
        )
    }
}

// endregion

private fun TextShimmerStyle.chipLabel(): String = when (this) {
    TextShimmerStyle.DISPLAY -> "Display"
    TextShimmerStyle.HEADING_MEDIUM -> "Head.M"
    TextShimmerStyle.HEADING_SMALL -> "Head.S"
    TextShimmerStyle.BODY -> "Body"
    TextShimmerStyle.SUBHEADING -> "Sub.H"
    TextShimmerStyle.CAPTION -> "Caption"
}

private fun RadiusOption.value(): Dp = when (this) {
    RadiusOption.R4 -> 4.dp
    RadiusOption.R8 -> 8.dp
    RadiusOption.R16 -> 16.dp
    RadiusOption.R24 -> 24.dp
    RadiusOption.R32 -> 32.dp
    RadiusOption.FULL -> 1000.dp
}

private fun RectangleWidthOption.value(): Dp = when (this) {
    RectangleWidthOption.W80 -> 80.dp
    RectangleWidthOption.W160 -> 160.dp
    RectangleWidthOption.W240 -> 240.dp
    RectangleWidthOption.FILL -> 0.dp // unused — handled separately
}

private fun RectangleHeightOption.value(): Dp = when (this) {
    RectangleHeightOption.H16 -> 16.dp
    RectangleHeightOption.H24 -> 24.dp
    RectangleHeightOption.H40 -> 40.dp
    RectangleHeightOption.H64 -> 64.dp
}

private const val SAMPLE_TEXT = "Sample shimmer text"