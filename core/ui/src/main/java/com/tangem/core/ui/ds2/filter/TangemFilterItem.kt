package com.tangem.core.ui.ds2.filter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_16
import com.tangem.core.ui.res.generated.icons.ic_cross_16

/**
 * Design-system v2 filter chip — a pill that either invites the user to pick a value or shows the
 * value already picked.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/branch/0xt9Tg8x8f0Z0m2KUdLG9q/%F0%9F%92%A0-DS-Components?node-id=7385-2463&m=dev)
 *
 * Usually placed inside a [TangemFilterGroup] rather than used standalone.
 *
 * Behavior notes:
 * - The chip has **two** click targets: clicking anywhere on it invokes `onClick` (open the list of
 *   options), while clicking the trailing cross of a [TangemFilterItemUM.Active] chip invokes
 *   `onClearClick` (drop the picked values). The cross has a slightly enlarged touch area that does
 *   not affect its visual position.
 * - [TangemFilterItemUM.Inactive] renders the filter name with a chevron;
 *   [TangemFilterItemUM.Active] renders the picked value, an optional `+N` counter and the cross.
 * - [TangemFilterItemUM.Loading] renders a fixed-width shimmer pill; [variant],
 *   [contentDescription] and clicks are ignored in that state.
 * - The label never wraps — it truncates with an ellipsis once a width constraint is applied via
 *   [modifier]. Without such a constraint the chip grows with its content.
 * - The active chip is inverse-colored: the material variant switches to the `material-inverted`
 *   token set, the transparent one to a flat `bg.inverse` fill.
 * - The press overlay comes from [TangemSurface]'s ripple and follows the background: the flat
 *   inverse-colored active chip is lightened, every other state is dimmed.
 * - While focused the chip is wrapped in the brand focus ring, which replaces its regular border.
 *
 * @param state Content and callbacks of the chip. See [TangemFilterItemUM].
 * @param modifier Modifier applied to the chip container. Constrain the width here (e.g.
 *   `Modifier.widthIn(max = 160.dp)`) to make long values truncate.
 * @param variant Visual style (Figma `APPEARANCE`). See [TangemFilterItem.Variant].
 * @param contentDescription Accessibility label announced by TalkBack for the chip. When non-null it
 *   overrides the label / value text; supply it when the visible text alone doesn't convey which
 *   filter this is (e.g. `"Network filter, Ethereum"`).
 * @param clearContentDescription Accessibility label of the trailing cross of an active chip. Supply
 *   it to announce the clear action (e.g. `"Clear network filter"`).
 * @param interactionSource Interaction source for press / focus state. A focused chip draws the
 *   brand focus ring around itself.
 */
@Composable
fun TangemFilterItem(
    state: TangemFilterItemUM,
    modifier: Modifier = Modifier,
    variant: TangemFilterItem.Variant = TangemFilterItem.Variant.Material,
    contentDescription: String? = null,
    clearContentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    if (state is TangemFilterItemUM.Loading) {
        TangemShimmer(
            modifier = modifier.size(width = ShimmerWidth, height = MinHeight),
            radius = PillRadius,
        )
        return
    }

    val colorTokens = resolveColorTokens(variant = variant, isActive = state is TangemFilterItemUM.Active)
    val isFocused by interactionSource.collectIsFocusedAsState()

    TangemSurface(
        modifier = modifier
            // Not merging descendants: the trailing cross of an active chip is a separate action and
            // has to stay reachable by TalkBack.
            .semantics {
                role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            }
            .heightIn(min = MinHeight)
            .widthIn(min = MinWidth),
        color = colorTokens.backgroundColor,
        isMaterial = colorTokens.isMaterial,
        materialStyle = colorTokens.materialStyle,
        border = resolveBorder(isFocused = isFocused, borderColor = colorTokens.borderColor),
        shape = CircleShape,
        onClick = state.clickHandler(),
        interactionSource = interactionSource,
    ) {
        FilterItemContent(
            state = state,
            colorTokens = colorTokens,
            clearContentDescription = clearContentDescription,
        )
    }
}

/** The focus ring replaces the chip's regular border while it is focused. */
@Composable
private fun resolveBorder(isFocused: Boolean, borderColor: Color?): BorderStroke? = when {
    isFocused -> BorderStroke(
        width = FocusRingWidth,
        color = TangemTheme.colors3.interaction.focusRing.brand,
    )
    borderColor != null -> BorderStroke(width = BorderWidth, color = borderColor)
    else -> null
}

@Composable
private fun FilterItemContent(
    state: TangemFilterItemUM,
    colorTokens: FilterColorTokens,
    clearContentDescription: String?,
) {
    val isActive = state is TangemFilterItemUM.Active

    Row(
        modifier = Modifier.padding(
            start = ContentStartPadding,
            // The cross sits inside an enlarged touch box, so the paddings around it are shrunk by
            // the same amount to keep the icon exactly where Figma puts it.
            end = if (isActive) ContentEndPadding - TouchExpansion else ContentEndPadding,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            if (isActive) TrailingIconSpacing - TouchExpansion else TrailingIconSpacing,
        ),
    ) {
        Row(
            // Vertical padding lives on the text wrapper rather than on the whole row so the cross
            // touch box may be taller than the text without growing the chip.
            modifier = Modifier.padding(vertical = ContentVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LabelCounterSpacing),
        ) {
            state.text()?.let { text ->
                FilterText(text = text, color = colorTokens.textColor)
            }
            (state as? TangemFilterItemUM.Active)?.counter?.let { counter ->
                FilterText(text = stringReference("+$counter"), color = colorTokens.counterColor)
            }
        }
        when (state) {
            is TangemFilterItemUM.Active -> ClearIcon(
                tint = colorTokens.iconTint,
                contentDescription = clearContentDescription,
                onClick = state.onClearClick,
            )
            is TangemFilterItemUM.Inactive -> Icon(
                modifier = Modifier.size(IconSize),
                imageVector = Icons.ic_chevron_down_16,
                tint = colorTokens.iconTint,
                contentDescription = null,
            )
            is TangemFilterItemUM.Loading -> Unit
        }
    }
}

@Composable
private fun FilterText(text: TextReference, color: Color) {
    Text(
        text = text.resolveReference(),
        color = color,
        style = TangemTheme.typography3.subheading.medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Trailing cross of an active chip. The icon keeps its 16dp visual size while the clickable box
 * around it is [TouchExpansion] bigger on every side, which the parent row compensates with smaller
 * paddings.
 */
@Composable
private fun ClearIcon(tint: Color, contentDescription: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(IconSize + TouchExpansion * 2)
            // Clipped so the press ripple stays round instead of flashing a square inside the pill.
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(IconSize),
            imageVector = Icons.ic_cross_16,
            tint = tint,
            contentDescription = contentDescription,
        )
    }
}

/** Click handler of the chip itself. Loading chips are non-interactive. */
private fun TangemFilterItemUM.clickHandler(): (() -> Unit)? = when (this) {
    is TangemFilterItemUM.Active -> onClick
    is TangemFilterItemUM.Inactive -> onClick
    is TangemFilterItemUM.Loading -> null
}

/** Text shown in the chip: the picked value for an active chip, the filter name otherwise. */
private fun TangemFilterItemUM.text(): TextReference? = when (this) {
    is TangemFilterItemUM.Active -> value
    is TangemFilterItemUM.Inactive -> label
    is TangemFilterItemUM.Loading -> null
}

object TangemFilterItem {

    /**
     * Visual style of the chip (Figma `APPEARANCE`).
     *
     * - [Material] — translucent haze fill with a gradient border, used over content backgrounds.
     * - [Transparent] — flat opaque fill, used over plain backgrounds.
     */
    enum class Variant {
        Material,
        Transparent,
    }
}

/** Resolved colors for a (variant, isActive) pair. */
private data class FilterColorTokens(
    val backgroundColor: Color,
    val textColor: Color,
    val counterColor: Color,
    val iconTint: Color,
    val isMaterial: Boolean = false,
    val materialStyle: TangemSurface.MaterialStyle = TangemSurface.MaterialStyle.Default,
    val borderColor: Color? = null,
)

@Composable
@ReadOnlyComposable
private fun resolveColorTokens(variant: TangemFilterItem.Variant, isActive: Boolean): FilterColorTokens {
    val colors = TangemTheme.colors3
    // The active chip is inverse-colored in both variants, so it always takes the inverse content
    // tokens; only the pill behind them differs.
    val activeContent = FilterColorTokens(
        backgroundColor = Color.Transparent,
        textColor = colors.text.inverse.primary,
        counterColor = colors.text.inverse.secondary,
        iconTint = colors.icon.inverse,
    )
    val inactiveContent = FilterColorTokens(
        backgroundColor = Color.Transparent,
        textColor = colors.text.secondary,
        counterColor = colors.text.secondary,
        iconTint = colors.icon.secondary,
    )
    return when {
        // In material mode TangemSurface ignores `backgroundColor` and paints the haze fill together
        // with its own gradient stroke, so here the state only selects the material token set:
        // `Inverted` is the `material-inverted` set the spec asks for on the active chip.
        variant == TangemFilterItem.Variant.Material -> (if (isActive) activeContent else inactiveContent).copy(
            isMaterial = true,
            materialStyle = if (isActive) {
                TangemSurface.MaterialStyle.Inverted
            } else {
                TangemSurface.MaterialStyle.Default
            },
        )
        isActive -> activeContent.copy(backgroundColor = colors.bg.inverse)
        else -> inactiveContent.copy(backgroundColor = colors.bg.opaque.secondary)
    }
}

private val MinHeight: Dp = 36.dp
private val MinWidth: Dp = 64.dp
private val PillRadius: Dp = 999.dp
private val BorderWidth: Dp = 1.dp
private val FocusRingWidth: Dp = 2.dp
private val IconSize: Dp = 16.dp
private val ContentStartPadding: Dp = 12.dp
private val ContentEndPadding: Dp = 10.dp
private val ContentVerticalPadding: Dp = 8.dp
private val TrailingIconSpacing: Dp = 6.dp
private val LabelCounterSpacing: Dp = 4.dp
private val ShimmerWidth: Dp = 80.dp

/** Extra touch area added around the trailing cross on every side. */
private val TouchExpansion: Dp = 4.dp

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemFilterItemPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TangemFilterItem.Variant.entries.forEach { variant ->
                PreviewVariantSection(variant = variant)
            }
        }
    }
}

@Composable
private fun PreviewVariantSection(variant: TangemFilterItem.Variant) {
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
            TangemFilterItem(state = PreviewInactive, variant = variant)
            TangemFilterItem(state = PreviewActive, variant = variant)
            TangemFilterItem(state = PreviewActiveWithCounter, variant = variant)
            TangemFilterItem(state = PreviewLoading, variant = variant)
        }
    }
}

private val PreviewInactive = TangemFilterItemUM.Inactive(
    id = "label",
    label = stringReference("Label"),
    onClick = {},
)

private val PreviewActive = TangemFilterItemUM.Active(
    id = "value",
    value = stringReference("Value"),
    onClick = {},
    onClearClick = {},
)

private val PreviewActiveWithCounter = TangemFilterItemUM.Active(
    id = "value_counter",
    value = stringReference("Value"),
    counter = 1,
    onClick = {},
    onClearClick = {},
)

private val PreviewLoading = TangemFilterItemUM.Loading(id = "loading")

// endregion