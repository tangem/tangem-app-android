package com.tangem.core.ui.ds2.row

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/** Controls how the title and value sides share horizontal space in a [TangemRow]. */
@Immutable
enum class TangemRowContentLead { Equal, Start, End }

/** Vertical alignment between the start slot, content labels, and end slot in a [TangemRow]. */
@Immutable
enum class TangemRowVerticalAlignment { Top, Center }

/**
 * Design-system v2 row: a list-item container with start / end slots, title+subtitle on the
 * leading side, value+subvalue on the trailing side, and an optional slot below the row.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=2344-1406)
 *
 * Usage:
 * ```
 * TangemRow(
 *     titleSlot = { TangemRowText("Network fee", TangemRowTextRole.Title) },
 *     valueSlot = { TangemRowText("0.0001 ETH", TangemRowTextRole.Value) },
 *     contentLead = TangemRowContentLead.Start,
 *     onClick = { /* ... */ },
 * )
 * ```
 *
 * @param modifier Modifier applied to the row container.
 * @param divider Draws an inset bottom divider.
 * @param includeInnerPaddings Applies the default row padding. Disable when wrapped in a container
 *   that already handles padding.
 * @param contentLead Strategy for sharing space between the title and value sides.
 *   See [TangemRowContentLead].
 * @param verticalAlignment Vertical alignment of the slots. See [TangemRowVerticalAlignment].
 * @param titleSlot Leading primary label.
 * @param subtitleSlot Leading secondary label rendered under [titleSlot].
 * @param valueSlot Trailing primary label.
 * @param subvalueSlot Trailing secondary label rendered under [valueSlot].
 * @param startSlot Leading icon / control slot.
 * @param endSlot Trailing icon / control slot.
 * @param extraBottomSlot Full-width content rendered below the row.
 * @param interactionSource Interaction source used when [onClick] is non-null.
 * @param onClick Click handler. `null` makes the row non-interactive.
 */
@Suppress("LongParameterList")
@Composable
fun TangemRow(
    modifier: Modifier = Modifier,
    divider: Boolean = false,
    includeInnerPaddings: Boolean = true,
    contentLead: TangemRowContentLead = TangemRowContentLead.Equal,
    verticalAlignment: TangemRowVerticalAlignment = TangemRowVerticalAlignment.Top,
    titleSlot: (@Composable RowScope.() -> Unit)? = null,
    subtitleSlot: (@Composable RowScope.() -> Unit)? = null,
    valueSlot: (@Composable RowScope.() -> Unit)? = null,
    subvalueSlot: (@Composable RowScope.() -> Unit)? = null,
    startSlot: (@Composable BoxScope.() -> Unit)? = null,
    endSlot: (@Composable BoxScope.() -> Unit)? = null,
    extraBottomSlot: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null,
) {
    val resolvedInteractionSource: MutableInteractionSource? = if (onClick != null) {
        interactionSource ?: remember { MutableInteractionSource() }
    } else {
        null
    }
    val isFocused = if (resolvedInteractionSource != null) {
        val isFocusedByState by resolvedInteractionSource.collectIsFocusedAsState()
        isFocusedByState
    } else {
        false
    }

    WithRowRipple(enabled = onClick != null) {
        Column(
            modifier = modifier
                .conditionalCompose(isFocused) { focusBorder() }
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = resolvedInteractionSource,
                            indication = LocalIndication.current,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .conditionalCompose(divider) {
                    bottomDivider(
                        color = TangemTheme.colors3.border.secondary,
                        width = 1.dp,
                        horizontalInset = 16.dp,
                    )
                }
                .conditionalCompose(includeInnerPaddings) {
                    padding(16.dp)
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = verticalAlignment.toCompose(),
            ) {
                SideSlot(slot = startSlot, position = SideSlotPosition.Start)
                ContentLabels(
                    modifier = Modifier.weight(1f),
                    contentLead = contentLead,
                    verticalAlignment = verticalAlignment,
                    titleSlot = titleSlot,
                    subtitleSlot = subtitleSlot,
                    valueSlot = valueSlot,
                    subvalueSlot = subvalueSlot,
                )
                SideSlot(slot = endSlot, position = SideSlotPosition.End)
            }
            if (extraBottomSlot != null) {
                SpacerH(8.dp)
                extraBottomSlot()
            }
        }
    }
}

@Composable
private fun WithRowRipple(enabled: Boolean, content: @Composable () -> Unit) {
    if (enabled) {
        CompositionLocalProvider(LocalRippleConfiguration provides tangemRowRipple(), content = content)
    } else {
        content()
    }
}

@Composable
@ReadOnlyComposable
private fun tangemRowRipple(): RippleConfiguration = RippleConfiguration(
    color = TangemTheme.colors3.interaction.press.default,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0f,
        focusedAlpha = 0f,
        hoveredAlpha = 0.05f,
        pressedAlpha = 0.1f,
    ),
)

private fun TangemRowVerticalAlignment.toCompose(): Alignment.Vertical = when (this) {
    TangemRowVerticalAlignment.Top -> Alignment.Top
    TangemRowVerticalAlignment.Center -> Alignment.CenterVertically
}

private enum class SideSlotPosition { Start, End }

@Composable
private fun SideSlot(slot: (@Composable BoxScope.() -> Unit)?, position: SideSlotPosition) {
    if (slot == null) return
    val spacing = 12.dp
    val padding = when (position) {
        SideSlotPosition.Start -> Modifier.padding(end = spacing)
        SideSlotPosition.End -> Modifier.padding(start = spacing)
    }
    Box(modifier = padding, content = slot)
}

/**
 * Lays out the title/value columns side-by-side with [contentLead]-aware width sharing.
 */
@Suppress("UnnecessaryParentheses", "LongParameterList")
@Composable
private fun ContentLabels(
    contentLead: TangemRowContentLead,
    verticalAlignment: TangemRowVerticalAlignment,
    titleSlot: (@Composable RowScope.() -> Unit)?,
    subtitleSlot: (@Composable RowScope.() -> Unit)?,
    valueSlot: (@Composable RowScope.() -> Unit)?,
    subvalueSlot: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val hasLeft = titleSlot != null || subtitleSlot != null
    val hasRight = valueSlot != null || subvalueSlot != null
    if (!hasLeft && !hasRight) return

    Layout(
        modifier = modifier,
        content = {
            // Always emit two roots so `measurables` indices are stable in the measure block.
            LabelColumnContent(primary = titleSlot, secondary = subtitleSlot, alignment = Alignment.Start)
            LabelColumnContent(primary = valueSlot, secondary = subvalueSlot, alignment = Alignment.End)
        },
    ) { measurables, constraints ->
        val titleMeasurable = measurables[0]
        val valueMeasurable = measurables[1]
        // Fall back to summed intrinsics when parent provides unbounded width — `layout()` cannot
        // report an infinite size.
        val rowWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            titleMeasurable.maxIntrinsicWidth(Int.MAX_VALUE) + valueMeasurable.maxIntrinsicWidth(Int.MAX_VALUE)
        }

        val (titleSlotWidth, valueSlotWidth) = when {
            !hasLeft -> 0 to rowWidth
            !hasRight -> rowWidth to 0
            else -> when (contentLead) {
                TangemRowContentLead.Equal -> (rowWidth / 2) to (rowWidth - rowWidth / 2)
                TangemRowContentLead.Start -> {
                    // Value hugs (intrinsic), title fills the rest. Cap the hugging side at half
                    // the row so the filling label never collapses to zero on overflow — degrades
                    // to an equal split when the hugging side wants more than half.
                    val v = valueMeasurable.maxIntrinsicWidth(Int.MAX_VALUE).coerceAtMost(rowWidth / 2)
                    (rowWidth - v) to v
                }
                TangemRowContentLead.End -> {
                    // Title hugs (intrinsic), value fills the rest. Same half-row cap as above.
                    val t = titleMeasurable.maxIntrinsicWidth(Int.MAX_VALUE).coerceAtMost(rowWidth / 2)
                    t to (rowWidth - t)
                }
            }
        }

        val titlePlaceable = titleMeasurable.measure(
            constraints.copy(minWidth = 0, maxWidth = titleSlotWidth),
        )
        val valuePlaceable = valueMeasurable.measure(
            constraints.copy(minWidth = 0, maxWidth = valueSlotWidth),
        )

        val rowHeight = maxOf(titlePlaceable.height, valuePlaceable.height)
        val yFor: (Int) -> Int = when (verticalAlignment) {
            TangemRowVerticalAlignment.Top -> { _ -> 0 }
            TangemRowVerticalAlignment.Center -> { h -> (rowHeight - h) / 2 }
        }

        layout(rowWidth, rowHeight) {
            titlePlaceable.placeRelative(x = 0, y = yFor(titlePlaceable.height))
            // Filling side is pinned to the row's trailing edge.
            valuePlaceable.placeRelative(
                x = rowWidth - valuePlaceable.width,
                y = yFor(valuePlaceable.height),
            )
        }
    }
}

@Composable
private fun LabelColumnContent(
    primary: (@Composable RowScope.() -> Unit)?,
    secondary: (@Composable RowScope.() -> Unit)?,
    alignment: Alignment.Horizontal,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = alignment,
    ) {
        if (primary != null) LabelRow(alignment = alignment, content = primary)
        if (secondary != null) LabelRow(alignment = alignment, content = secondary)
    }
}

@Composable
private fun LabelRow(alignment: Alignment.Horizontal, content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = alignment,
        ),
        content = content,
    )
}

/** Semantic role of a label inside a [TangemRow], driving its typography, color and alignment. */
@Immutable
enum class TangemRowTextRole { Title, Subtitle, Value, Subvalue }

/**
 * Default text styling for [TangemRow] label slots.
 *
 * Usage:
 * ```
 * TangemRowText(text = "Network fee", role = TangemRowTextRole.Title)
 * ```
 *
 * @param text Label text.
 * @param role Semantic role. See [TangemRowTextRole].
 * @param modifier Modifier applied to the underlying [Text].
 * @param maxLines Maximum number of visible lines before truncation.
 * @param overflow Overflow behavior. Defaults to ellipsis.
 */
@Composable
fun TangemRowText(
    text: String,
    role: TangemRowTextRole,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    Text(
        text = text,
        modifier = modifier,
        color = rowTextColor(role),
        style = rowTextStyle(role),
        textAlign = rowTextAlign(role),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * Default text styling for [TangemRow] label slots.
 *
 * @param text Label text.
 * @param role Semantic role. See [TangemRowTextRole].
 * @param modifier Modifier applied to the underlying [Text].
 * @param maxLines Maximum number of visible lines before truncation.
 * @param overflow Overflow behavior. Defaults to ellipsis.
 */
@Composable
@NonRestartableComposable
fun TangemRowText(
    text: TextReference,
    role: TangemRowTextRole,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    TangemRowText(
        text = text.resolveReference(),
        role = role,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
private fun rowTextStyle(role: TangemRowTextRole): TextStyle = when (role) {
    TangemRowTextRole.Title, TangemRowTextRole.Value -> TangemTheme.typography3.body.medium
    TangemRowTextRole.Subtitle, TangemRowTextRole.Subvalue -> TangemTheme.typography3.caption.medium
}

@Composable
private fun rowTextColor(role: TangemRowTextRole): Color = when (role) {
    TangemRowTextRole.Title, TangemRowTextRole.Value -> TangemTheme.colors3.text.primary
    TangemRowTextRole.Subtitle, TangemRowTextRole.Subvalue -> TangemTheme.colors3.text.secondary
}

private fun rowTextAlign(role: TangemRowTextRole): TextAlign = when (role) {
    TangemRowTextRole.Title, TangemRowTextRole.Subtitle -> TextAlign.Start
    TangemRowTextRole.Value, TangemRowTextRole.Subvalue -> TextAlign.End
}

@Composable
private fun Modifier.focusBorder(): Modifier {
    val radius = 16.dp
    val shape = remember(radius) { RoundedCornerShape(radius) }
    return border(
        width = 2.dp,
        color = TangemTheme.colors3.interaction.focusRing.default,
        shape = shape,
    )
}

private fun Modifier.bottomDivider(color: Color, width: Dp, horizontalInset: Dp): Modifier = drawWithContent {
    drawContent()
    val stroke = width.toPx()
    val inset = horizontalInset.toPx()
    val y = size.height - stroke / 2f
    drawLine(
        color = color,
        start = Offset(x = inset, y = y),
        end = Offset(x = size.width - inset, y = y),
        strokeWidth = stroke,
    )
}

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemRowPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(vertical = 16.dp),
        ) {
            // Equal lead, text only, with divider
            TangemRow(
                divider = true,
                contentLead = TangemRowContentLead.Equal,
                titleSlot = { TangemRowText(text = "Title", role = TangemRowTextRole.Title) },
                subtitleSlot = { TangemRowText(text = "Subtitle", role = TangemRowTextRole.Subtitle) },
                valueSlot = { TangemRowText(text = "Value", role = TangemRowTextRole.Value) },
                subvalueSlot = { TangemRowText(text = "Subvalue", role = TangemRowTextRole.Subvalue) },
            )
            // Start lead — title hugs, value side fills
            TangemRow(
                divider = true,
                contentLead = TangemRowContentLead.Start,
                titleSlot = { TangemRowText(text = "Network fee", role = TangemRowTextRole.Title) },
                valueSlot = { TangemRowText(text = "0.0001 ETH", role = TangemRowTextRole.Value) },
                subvalueSlot = { TangemRowText(text = "≈ $0.32", role = TangemRowTextRole.Subvalue) },
            )
            // End lead — value hugs, title side fills
            TangemRow(
                divider = true,
                contentLead = TangemRowContentLead.End,
                titleSlot = {
                    TangemRowText(
                        text = "Recipient with a very long address that should ellipsize",
                        role = TangemRowTextRole.Title,
                    )
                },
                valueSlot = { TangemRowText(text = "0x1234…abcd", role = TangemRowTextRole.Value) },
            )
            // Side slots + clickable + centered alignment
            TangemRow(
                verticalAlignment = TangemRowVerticalAlignment.Center,
                startSlot = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_chevron_24),
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.primary,
                    )
                },
                titleSlot = { TangemRowText(text = "Settings", role = TangemRowTextRole.Title) },
                subtitleSlot = {
                    TangemRowText(text = "Tap to configure", role = TangemRowTextRole.Subtitle)
                },
                endSlot = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_chevron_24),
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.secondary,
                    )
                },
                onClick = {},
            )
            // Title only — exercises nullable slots / single LabelRow path
            TangemRow(
                titleSlot = { TangemRowText(text = "Single-line row", role = TangemRowTextRole.Title) },
            )
        }
    }
}

// endregion