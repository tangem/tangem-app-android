package com.tangem.core.ui.ds2.tabnavigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 tab item — a selectable pill inside a tab navigation row.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=6587-3310)
 *
 * Behavior notes:
 * - An unselected tab has no background of its own; only the selected tab is filled.
 * - The label color morphs on the three timings Figma specifies: it dims within 100ms while pressed,
 *   returns over the next 100ms on release, and lights up on selection only after a 200ms delay so a
 *   shared selection pill has time to arrive first.
 * - A selected tab never dims under the finger.
 * - Focus draws the brand ring in place of the pill's border.
 *
 * @param state Content, selection and callbacks of the tab. `selected` drives the label color and,
 *   when [background] is [TangemTabItem.Background.Own], the pill fill. See [TangemTabItemUM].
 * @param modifier Modifier applied to the pill. Constrain the width here (e.g.
 *   `Modifier.widthIn(max = 120.dp)`) to make long labels truncate.
 * @param variant Visual style of the selected pill (Figma `APPEARANCE`). See
 *   [TangemTabItem.Variant].
 * @param background Who paints the selected pill. See [TangemTabItem.Background].
 * @param contentDescription Accessibility label announced by TalkBack. When non-null it replaces the
 *   label and counter text; supply it when those alone are ambiguous (e.g. `"Staking, 21 positions"`).
 * @param interactionSource Interaction source for press / focus state.
 */
@Composable
fun TangemTabItem(
    state: TangemTabItemUM,
    modifier: Modifier = Modifier,
    variant: TangemTabItem.Variant = TangemTabItem.Variant.Material,
    background: TangemTabItem.Background = TangemTabItem.Background.Own,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    if (state is TangemTabItemUM.Loading) {
        TangemShimmer(
            modifier = modifier
                .size(width = ShimmerWidth, height = ItemHeight)
                .clearAndSetSemantics {},
            radius = PillRadius,
        )
        return
    }

    val isSelected = state is TangemTabItemUM.Content && state.isSelected
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val fill = resolveFill(variant = variant, selected = isSelected, background = background)
    val contentColor = animateContentColor(selected = isSelected, isPressed = isPressed)
    val onClick = state.clickHandler().withSelectionTick(selected = isSelected)

    TangemSurface(
        modifier = modifier
            // Merging is what lets this Role.Tab win over the Role.Button of the clickable inside
            // TangemSurface.
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = isSelected
                contentDescription?.let { this.contentDescription = it }
            }
            .height(ItemHeight)
            .widthIn(min = MinWidth)
            // Clicked here rather than through TangemSurface's own onClick, which would draw a ripple:
            // the spec gives a pressed tab no background of its own, only the label's color morph.
            .conditional(onClick != null) {
                clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick ?: {},
                )
            },
        color = fill.color,
        isMaterial = fill.isMaterial,
        border = resolveBorder(isFocused = isFocused),
        shape = CircleShape,
        interactionSource = interactionSource,
    ) {
        TabItemContent(state = state, contentColor = contentColor)
    }
}

@Composable
private fun TabItemContent(state: TangemTabItemUM, contentColor: Color) {
    Row(
        modifier = Modifier.padding(horizontal = ContentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (state as? TangemTabItemUM.Content)?.iconStart?.let { icon ->
            TangemIcon(
                tangemIconUM = icon.withTint(contentColor),
                modifier = Modifier.size(IconSize),
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = LabelPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LabelCounterSpacing),
        ) {
            TabText(text = state.label(), color = contentColor)

            (state as? TangemTabItemUM.Content)?.counter?.let { counter ->
                TabText(text = counter, color = TangemTheme.colors3.text.tertiary)
            }
        }
    }
}

@Composable
private fun TabText(text: TextReference, color: Color) {
    Text(
        text = text.resolveReference(),
        color = color,
        style = TangemTheme.typography3.subheading.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * The `(selected, isPressed)` pair picks both the destination color and the spec to reach it with, so
 * no bookkeeping of which input changed is needed.
 *
 * Both `when`s test `selected` first, and they have to stay that way: selection normally flips while
 * the finger is still down, and if the spec branch let `isPressed` win there, the tab would light up
 * on the 100ms press morph instead of waiting out the pill's travel on the delayed activation morph.
 */
@Composable
private fun animateContentColor(selected: Boolean, isPressed: Boolean): Color {
    val colors = TangemTheme.colors3
    val target = when {
        selected -> colors.text.primary
        isPressed -> colors.text.tertiary
        else -> colors.text.secondary
    }
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = when {
            selected -> ContentActivationSpec
            isPressed -> ContentPressSpec
            else -> ContentReleaseSpec
        },
        label = "tabItemContentColor",
    )
    return color
}

@Composable
private fun resolveBorder(isFocused: Boolean): BorderStroke? = when {
    isFocused -> BorderStroke(
        width = FocusRingWidth,
        color = TangemTheme.colors3.interaction.focusRing.brand,
    )
    else -> null
}

private data class TabFill(val color: Color, val isMaterial: Boolean = false)

@Composable
@ReadOnlyComposable
private fun resolveFill(
    variant: TangemTabItem.Variant,
    selected: Boolean,
    background: TangemTabItem.Background,
): TabFill {
    val isPainted = selected && background == TangemTabItem.Background.Own
    return when {
        !isPainted -> TabFill(color = Color.Transparent)
        variant == TangemTabItem.Variant.Material -> TabFill(color = Color.Transparent, isMaterial = true)
        else -> TabFill(color = TangemTheme.colors3.bg.opaque.secondary)
    }
}

private fun TangemTabItemUM.clickHandler(): (() -> Unit)? = when (this) {
    is TangemTabItemUM.Content -> onClick
    is TangemTabItemUM.Loading -> null
}

/** Ticks only when the tap will actually move the selection, so re-tapping the active tab is silent. */
@Composable
private fun (() -> Unit)?.withSelectionTick(selected: Boolean): (() -> Unit)? {
    val hapticManager = LocalHapticManager.current
    val handler = this ?: return null
    return {
        if (!selected) hapticManager.perform(TangemHapticEffect.View.SegmentTick)
        handler()
    }
}

private fun TangemTabItemUM.label(): TextReference = when (this) {
    is TangemTabItemUM.Content -> label
    is TangemTabItemUM.Loading -> TextReference.EMPTY
}

/**
 * [TangemIconUM.Icon]'s convenience constructor defaults `tint` to a non-null reference, so a
 * caller-supplied tint can't be told apart from "no tint supplied" — the tab's own color always wins.
 */
private fun TangemIconUM.withTint(color: Color): TangemIconUM = when (this) {
    is TangemIconUM.Icon -> copy(tint = ColorReference2 { color })
    else -> this
}

object TangemTabItem {

    /**
     * Visual style of the selected pill (Figma `APPEARANCE`).
     *
     * - [Material] — translucent glass with a gradient stroke and a soft shadow. Use when the tab row
     *   is pinned to the top or bottom edge, over scrolling content.
     * - [Transparent] — flat opaque fill. Use when the tab row sits inline in page content.
     */
    enum class Variant {
        Material,
        Transparent,
    }

    /**
     * Who paints the pill behind a selected tab.
     *
     * - [Own] — the tab paints it itself. Use when rendering a tab standalone.
     * - [Hoisted] — the tab paints only its label; something above it draws one shared pill for the
     *   whole row. This is what the tab navigation container passes, so that the pill can animate
     *   between tabs instead of crossfading in place.
     */
    enum class Background {
        Own,
        Hoisted,
    }
}

internal val ItemHeight: Dp = 36.dp
private val MinWidth: Dp = 56.dp
private val ShimmerWidth: Dp = 64.dp
private val PillRadius: Dp = 999.dp
private val FocusRingWidth: Dp = 2.dp
private val IconSize: Dp = 20.dp
private val ContentPadding: Dp = 8.dp
private val LabelPadding: Dp = 4.dp
private val LabelCounterSpacing: Dp = 4.dp

// region Figma: 💠 Tab Navigation → // Animation

private const val CONTENT_MORPH_DURATION_MS = 100
private const val CONTENT_ACTIVATION_DELAY_MS = 200
private const val CONTENT_ACTIVATION_DURATION_MS = 200

private val ContentMorphEasing = CubicBezierEasing(a = 0.8f, b = 0f, c = 0.6f, d = 1f)

/** `TAB ITEM PRESS`: 0 → 100ms. */
private val ContentPressSpec: AnimationSpec<Color> = tween(
    durationMillis = CONTENT_MORPH_DURATION_MS,
    easing = ContentMorphEasing,
)

/** `TAB ITEM PRESS`, second half: 100 → 200ms, i.e. the same morph running after the press ends. */
private val ContentReleaseSpec: AnimationSpec<Color> = ContentPressSpec

/** `TAB ITEM ACTIVATION`: 200 → 400ms — the label lights up only once the pill has arrived. */
private val ContentActivationSpec: AnimationSpec<Color> = tween(
    durationMillis = CONTENT_ACTIVATION_DURATION_MS,
    delayMillis = CONTENT_ACTIVATION_DELAY_MS,
    easing = ContentMorphEasing,
)

// endregion

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemTabItemPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TangemTabItem.Variant.entries.forEach { variant ->
                PreviewVariantSection(variant = variant)
            }
        }
    }
}

@Composable
private fun PreviewVariantSection(variant: TangemTabItem.Variant) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = variant.name,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TangemTabItem(state = PreviewSelected, variant = variant)
            TangemTabItem(state = PreviewLabel, variant = variant)
            TangemTabItem(state = PreviewWithCounter, variant = variant)
            TangemTabItem(state = PreviewLoading, variant = variant)
        }
    }
}

private val PreviewSelected = TangemTabItemUM.Content(
    id = "selected",
    label = stringReference("Label"),
    isSelected = true,
    onClick = {},
)

private val PreviewLabel = TangemTabItemUM.Content(
    id = "label",
    label = stringReference("Label"),
    onClick = {},
)

private val PreviewWithCounter = TangemTabItemUM.Content(
    id = "counter",
    label = stringReference("Staking"),
    counter = stringReference("21"),
    onClick = {},
)

private val PreviewLoading = TangemTabItemUM.Loading(id = "loading")

// endregion