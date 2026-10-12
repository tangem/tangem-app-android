@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.expandablerow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.expandable.ExpandableAvatar
import com.tangem.core.ui.ds2.expandable.ExpandableCaptionText
import com.tangem.core.ui.ds2.expandable.ExpandableCard
import com.tangem.core.ui.ds2.expandable.ExpandableCardLayoutId
import com.tangem.core.ui.ds2.expandable.ExpandableCardShape
import com.tangem.core.ui.ds2.expandable.ExpandableCollapseArrow
import com.tangem.core.ui.ds2.expandable.ExpandableDot
import com.tangem.core.ui.ds2.expandable.ExpandableEmptyBody
import com.tangem.core.ui.ds2.expandable.ExpandableShimmerLine
import com.tangem.core.ui.ds2.expandable.ExpandableTitleAnchor
import com.tangem.core.ui.ds2.expandable.ExpansionMotion
import com.tangem.core.ui.ds2.expandable.rememberExpansionMotion
import com.tangem.core.ui.ds2.expandable.rememberRowPressAlpha
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 (DS3) **Expandable Row** — the generic sibling of `TangemAccountRow`: a card that
 * is a summary line while collapsed (avatar, title, optional badge, subtitle, value and caption) and
 * shrinks that line into a compact heading when expanded.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10496-1617)
 *
 * Behavior notes:
 * - **The row is the heading only.** What it reveals when it expands is emitted by the host next to
 *   it, so a `LazyColumn` keeps composing only what is on screen. Continue the card downwards by
 *   passing a [shape] whose bottom corners are square and giving the sibling items the same
 *   background — see `Modifier.roundedShapeItemDecoration`.
 * - [isExpanded] is an input, not internal state — the host decides which row is open. Toggle it
 *   from [onClick].
 * - Expanding is a single continuous morph, not a cross-fade: the avatar shrinks 40dp → 16dp and the
 *   title travels up and scales to caption size, while the value, caption and badge dissolve and the
 *   compact `Title · Value · Caption` run and the collapse arrow fade in behind them. The row has no
 *   animation spec of its own in Figma — it shares the account row's choreography.
 * - The title is always one line and ellipsized, in both states.
 * - Holding the row down dims its heading to 60 %.
 *
 * @param avatar Leading avatar. The row renders it itself, morphing it from 40dp to 16dp as it
 * expands — pass the state, not a pre-sized composable. See [TangemAvatar.UiState].
 * @param title Row title. Single line, ellipsized.
 * @param modifier Modifier applied to the card. The card always fills the available width.
 * @param badge Localized label of the badge beside the title, shown while collapsed. `null` hides it.
 * @param subtitle Secondary line under the title, shown while collapsed. `null` hides it.
 * @param value Primary value at the end of the collapsed row; joins the compact heading run while
 * expanded. `null` hides it and the dot separating it from the title.
 * @param caption Secondary line under [value]; joins the compact heading run while expanded. `null`
 * hides it and the dot before it.
 * @param isExpandable Whether the row can be opened at all. `false` hides the collapse arrow and
 * pins the row to its collapsed geometry.
 * @param isExpanded Whether the row is open. Ignored unless [isExpandable].
 * @param shape Card background and clip. Keep the default for a standalone row; pass a shape with
 * square bottom corners when sibling items continue the card below.
 * @param collapseContentDescription Accessibility label of the collapse arrow; supply it when the
 * row is expandable so the affordance is announced.
 * @param onClick Card click handler. `null` with no [onLongClick] makes the card non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 */
@Suppress("LongParameterList")
@Composable
fun TangemExpandableRow(
    avatar: TangemAvatar.UiState,
    title: TextReference,
    modifier: Modifier = Modifier,
    badge: TextReference? = null,
    subtitle: TextReference? = null,
    value: TextReference? = null,
    caption: TextReference? = null,
    isExpandable: Boolean = false,
    isExpanded: Boolean = false,
    shape: Shape = ExpandableCardShape,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    ExpandableRowScaffold(
        avatar = avatar,
        title = title,
        modifier = modifier,
        badge = badge,
        subtitle = subtitle,
        value = value,
        caption = caption,
        isExpandable = isExpandable,
        isExpanded = isExpanded,
        shape = shape,
        collapseContentDescription = collapseContentDescription,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

/** Public API surface of [TangemExpandableRow]. */
object TangemExpandableRow {

    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /**
     * State model of [TangemExpandableRow] — one subtype per Figma variant. Render it with the
     * `TangemExpandableRow(state = …)` overload.
     */
    @Immutable
    sealed class State {

        /** Unique id, e.g. for `LazyColumn` item keys. */
        abstract val id: String

        /**
         * Default variant — value and caption at the end of the collapsed row.
         *
         * @param id Unique id.
         * @param title Row title.
         * @param badge Localized badge label beside the title. `null` hides it.
         * @param subtitle Secondary line under the title. `null` hides it.
         * @param value Primary value at the end. `null` hides it and its separator dot.
         * @param caption Secondary line under [value]. `null` hides it and its separator dot.
         * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
         * @param onLongClick Card long-press handler. `null` disables long-press.
         */
        data class Content(
            override val id: String,
            val title: TextReference,
            val badge: TextReference? = null,
            val subtitle: TextReference? = null,
            val value: TextReference? = null,
            val caption: TextReference? = null,
            val onClick: (() -> Unit)? = null,
            val onLongClick: (() -> Unit)? = null,
        ) : State()

        /**
         * Loading variant — avatar and text-line shimmers in the collapsed geometry.
         *
         * @param id Unique id.
         */
        data class Shimmer(
            override val id: String,
        ) : State()
    }
}

/**
 * Design-system v2 (DS3) **Expandable Row** — state-driven overload: renders the variant described
 * by [TangemExpandableRow.State].
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10496-1617)
 *
 * @param avatar Leading avatar. The row renders it itself, morphing it from 40dp to 16dp as it
 * expands — pass the state, not a pre-sized composable. See [TangemAvatar.UiState].
 * @param state Card state model. See [TangemExpandableRow.State].
 * @param modifier Modifier applied to the card.
 * @param isExpandable Whether the row can be opened at all.
 * @param isExpanded Whether the row is open. Ignored unless [isExpandable].
 * @param shape Card background and clip. Pass a shape with square bottom corners when sibling items
 * continue the card below.
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 */
@Suppress("LongParameterList")
@Composable
fun TangemExpandableRow(
    avatar: TangemAvatar.UiState,
    state: TangemExpandableRow.State,
    modifier: Modifier = Modifier,
    isExpandable: Boolean = false,
    isExpanded: Boolean = false,
    shape: Shape = ExpandableCardShape,
    collapseContentDescription: String? = null,
) {
    when (state) {
        is TangemExpandableRow.State.Content -> TangemExpandableRow(
            avatar = avatar,
            title = state.title,
            modifier = modifier,
            badge = state.badge,
            subtitle = state.subtitle,
            value = state.value,
            caption = state.caption,
            isExpandable = isExpandable,
            isExpanded = isExpanded,
            shape = shape,
            collapseContentDescription = collapseContentDescription,
            onClick = state.onClick,
            onLongClick = state.onLongClick,
        )
        is TangemExpandableRow.State.Shimmer -> TangemExpandableRow.Shimmer(
            modifier = modifier,
            shape = shape,
        )
    }
}

/**
 * Design-system v2 (DS3) **Expandable Row / Empty body** — the prompt shown under an open row that
 * has nothing to reveal yet.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10496-1617)
 *
 * Place it as the sibling that follows the row, the same way the rows it stands in for are placed,
 * and give it a [shape] that closes the card.
 *
 * @param message Localized prompt.
 * @param addTokensLabel Localized label of the button under [message].
 * @param onAddTokensClick Invoked when that button is tapped.
 * @param modifier Modifier applied to the body. It always fills the available width.
 * @param shape Background and clip. Defaults to the standalone 24dp card; under a row, pass a shape
 * whose top corners are square.
 */
@Composable
fun TangemExpandableRow.EmptyBody(
    message: TextReference,
    addTokensLabel: TextReference,
    onAddTokensClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ExpandableCardShape,
) {
    ExpandableEmptyBody(
        message = message,
        addTokensLabel = addTokensLabel,
        onAddTokensClick = onAddTokensClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TangemTheme.colors3.bg.secondary),
    )
}

/**
 * Design-system v2 (DS3) **Expandable Row / Shimmer** — loading placeholder in the collapsed
 * geometry.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10496-1617)
 *
 * @param modifier Modifier applied to the card.
 * @param shape Card background and clip.
 */
@Composable
fun TangemExpandableRow.Shimmer(modifier: Modifier = Modifier, shape: Shape = ExpandableCardShape) {
    val interactionSource = remember { MutableInteractionSource() }
    ExpandableCard(
        motion = rememberExpansionMotion(isExpanded = false),
        interactionSource = interactionSource,
        modifier = modifier,
        contentGap = 12.dp,
        shape = shape,
    ) {
        TangemAvatar(
            state = TangemAvatar.UiState.Shimmer(shape = TangemAvatar.UiState.Shimmer.Shape.RoundedSquare),
            size = TangemAvatar.Size.X40,
            modifier = Modifier.layoutId(ExpandableCardLayoutId.AVATAR),
        )
        ExpandableShimmerLine(
            lineHeight = 20.dp,
            width = 70.dp,
            modifier = Modifier.layoutId(ExpandableCardLayoutId.TITLE),
        )
        ExpandableShimmerLine(
            lineHeight = 16.dp,
            width = 42.dp,
            modifier = Modifier.layoutId(ExpandableCardLayoutId.SUBTITLE),
        )
        ExpandableShimmerLine(
            lineHeight = 20.dp,
            width = 70.dp,
            modifier = Modifier.layoutId(ExpandableCardLayoutId.END_TOP),
        )
        ExpandableShimmerLine(
            lineHeight = 16.dp,
            width = 42.dp,
            modifier = Modifier.layoutId(ExpandableCardLayoutId.END_BOTTOM),
        )
    }
}

/**
 * Design-system v2 (DS3) **Expandable Row / Content Type = Slot** — the caller supplies the row's
 * own content instead of the title/subtitle/value/caption set: [contentSlot] fills the collapsed
 * line beside the avatar, [headingSlot] takes its place in the compact expanded heading.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10496-1617)
 *
 * The card still owns the morph: both slots are placed at the interpolated anchors and cross-faded
 * on the shared choreography, so a slot only has to render its own content at its own size.
 *
 * @param avatar Leading avatar. The row renders it itself, morphing it from 40dp to 16dp as it
 * expands. See [TangemAvatar.UiState].
 * @param modifier Modifier applied to the card.
 * @param isExpandable Whether the row can be opened at all.
 * @param isExpanded Whether the row is open. Ignored unless [isExpandable].
 * @param shape Card background and clip. Pass a shape with square bottom corners when sibling items
 * continue the card below.
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param onClick Card click handler. `null` with no [onLongClick] makes the card non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 * @param headingSlot Compact heading content, shown while expanded. Keep it to one 20dp line — it is
 * laid out beside the 16dp avatar and the collapse arrow.
 * @param contentSlot Collapsed content, shown beside the 40dp avatar as a row with 8dp between its
 * children. It spans the whole line, so put a start/end split in it with `Modifier.weight(1f)` on the
 * leading child — the arrangement the default variant's content start and end columns take.
 */
@Suppress("LongParameterList")
@Composable
fun TangemExpandableRow.Slot(
    avatar: TangemAvatar.UiState,
    modifier: Modifier = Modifier,
    isExpandable: Boolean = false,
    isExpanded: Boolean = false,
    shape: Shape = ExpandableCardShape,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    headingSlot: @Composable () -> Unit = {},
    contentSlot: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressAlpha by rememberRowPressAlpha(interactionSource)
    val motion = rememberExpansionMotion(isExpanded = isExpanded && isExpandable)

    ExpandableCard(
        motion = motion,
        interactionSource = interactionSource,
        modifier = modifier,
        contentGap = 8.dp,
        shape = shape,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        ExpandableAvatar(
            state = avatar,
            motion = motion,
            modifier = Modifier
                .layoutId(ExpandableCardLayoutId.AVATAR)
                .graphicsLayer { alpha = pressAlpha },
        )

        Row(
            modifier = Modifier
                .layoutId(ExpandableCardLayoutId.CONTENT_SLOT)
                .graphicsLayer { alpha = pressAlpha * motion.subtitleAlpha },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = contentSlot,
        )

        Row(
            modifier = Modifier
                .layoutId(ExpandableCardLayoutId.HEADING_SLOT)
                .offset(x = motion.compactOffset)
                .graphicsLayer { alpha = pressAlpha * motion.compactAlpha },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            headingSlot()
        }

        CollapseArrow(
            motion = motion,
            pressAlpha = pressAlpha,
            isExpandable = isExpandable,
            collapseContentDescription = collapseContentDescription,
        )
    }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun ExpandableRowScaffold(
    avatar: TangemAvatar.UiState,
    title: TextReference,
    modifier: Modifier = Modifier,
    badge: TextReference? = null,
    subtitle: TextReference? = null,
    value: TextReference? = null,
    caption: TextReference? = null,
    isExpandable: Boolean = false,
    isExpanded: Boolean = false,
    shape: Shape = ExpandableCardShape,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressAlpha by rememberRowPressAlpha(interactionSource)
    val motion = rememberExpansionMotion(isExpanded = isExpanded && isExpandable)

    ExpandableCard(
        motion = motion,
        interactionSource = interactionSource,
        modifier = modifier,
        contentGap = 8.dp,
        shape = shape,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        ExpandableAvatar(
            state = avatar,
            motion = motion,
            modifier = Modifier
                .layoutId(ExpandableCardLayoutId.AVATAR)
                .graphicsLayer { alpha = pressAlpha },
        )

        ExpandableRowTitleLine(
            title = title,
            motion = motion,
            badge = badge,
            badgeAlpha = pressAlpha * motion.subtitleAlpha,
            modifier = Modifier
                .layoutId(ExpandableCardLayoutId.TITLE)
                .graphicsLayer { alpha = pressAlpha },
        )
        ExpandableTitleAnchor(title = title, modifier = Modifier.layoutId(ExpandableCardLayoutId.TITLE_ANCHOR))

        if (subtitle != null) {
            ExpandableCaptionText(
                text = subtitle,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.SUBTITLE)
                    .graphicsLayer { alpha = pressAlpha * motion.subtitleAlpha },
            )
        }

        if (value != null) {
            ExpandableRowValue(
                value = value,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.END_TOP)
                    .graphicsLayer { alpha = pressAlpha * motion.endTopAlpha },
            )
            ExpandableDot(
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.INLINE_1)
                    .offset(x = motion.compactOffset)
                    .graphicsLayer { alpha = pressAlpha * motion.dotAlpha },
            )
            ExpandableRowCompactValue(
                value = value,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.INLINE_2)
                    .offset(x = motion.compactOffset)
                    .graphicsLayer { alpha = pressAlpha * motion.compactAlpha },
            )
        }

        if (caption != null) {
            ExpandableCaptionText(
                text = caption,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.END_BOTTOM)
                    .graphicsLayer { alpha = pressAlpha * motion.endBottomAlpha },
            )
            ExpandableDot(
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.INLINE_3)
                    .offset(x = motion.compactOffset)
                    .graphicsLayer { alpha = pressAlpha * motion.dotAlpha },
            )
            ExpandableCaptionText(
                text = caption,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.INLINE_4)
                    .offset(x = motion.compactOffset)
                    .graphicsLayer { alpha = pressAlpha * motion.compactAlpha },
            )
        }

        CollapseArrow(
            motion = motion,
            pressAlpha = pressAlpha,
            isExpandable = isExpandable,
            collapseContentDescription = collapseContentDescription,
        )
    }
}

@Composable
private fun CollapseArrow(
    motion: ExpansionMotion,
    pressAlpha: Float,
    isExpandable: Boolean,
    collapseContentDescription: String?,
) {
    if (!isExpandable) return

    ExpandableCollapseArrow(
        contentDescription = collapseContentDescription,
        modifier = Modifier
            .layoutId(ExpandableCardLayoutId.ARROW)
            .graphicsLayer { alpha = pressAlpha * motion.arrowAlpha },
    )
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemExpandableRowCollapsedPreview() {
    PreviewContainer { avatar ->
        TangemExpandableRow(
            avatar = avatar,
            title = stringReference("Title"),
            badge = stringReference("Label"),
            subtitle = stringReference("Subtitle"),
            value = stringReference("Value"),
            caption = stringReference("Caption"),
            isExpandable = true,
            onClick = {},
        )
        TangemExpandableRow(
            avatar = avatar,
            title = stringReference("Title"),
            subtitle = stringReference("Subtitle"),
            value = stringReference("Value"),
        )
        TangemExpandableRow.Slot(avatar = avatar) {
            ExpandableCaptionText(text = stringReference("Content start"), modifier = Modifier.weight(1f))
            ExpandableCaptionText(text = stringReference("Content end"))
        }
        TangemExpandableRow.Shimmer()
    }
}

@Preview(name = "Expanded Light", showBackground = true, widthDp = 360)
@Preview(name = "Expanded Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemExpandableRowExpandedPreview() {
    PreviewContainer { avatar ->
        PreviewGroup {
            TangemExpandableRow(
                avatar = avatar,
                title = stringReference("Title"),
                badge = stringReference("Label"),
                subtitle = stringReference("Subtitle"),
                value = stringReference("Value"),
                caption = stringReference("Caption"),
                isExpandable = true,
                isExpanded = true,
                shape = PreviewOpenShape,
                collapseContentDescription = "Collapse",
                onClick = {},
            )
            PreviewTokenRow()
        }
        PreviewGroup {
            TangemExpandableRow(
                avatar = avatar,
                title = stringReference("Title"),
                value = stringReference("Value"),
                caption = stringReference("Caption"),
                isExpandable = true,
                isExpanded = true,
                shape = PreviewOpenShape,
                collapseContentDescription = "Collapse",
                onClick = {},
            )
            TangemExpandableRow.EmptyBody(
                message = stringReference("Add tokens to begin tracking your crypto assets and transactions"),
                addTokensLabel = stringReference("Add tokens"),
                onAddTokensClick = {},
                shape = PreviewClosingShape,
            )
        }
        PreviewGroup {
            TangemExpandableRow.Slot(
                avatar = avatar,
                isExpandable = true,
                isExpanded = true,
                shape = PreviewOpenShape,
                collapseContentDescription = "Collapse",
                onClick = {},
                headingSlot = { ExpandableCaptionText(text = stringReference("Custom heading slot")) },
                contentSlot = {
                    ExpandableCaptionText(text = stringReference("Content start"), modifier = Modifier.weight(1f))
                    ExpandableCaptionText(text = stringReference("Content end"))
                },
            )
            PreviewTokenRow()
        }
    }
}

/** Card shape of a row that sibling items continue below it. */
private val PreviewOpenShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
)

/** Card shape of the item that closes the group. */
private val PreviewClosingShape = RoundedCornerShape(
    bottomStart = 24.dp,
    bottomEnd = 24.dp,
)

/**
 * Stands in for the host's list: the row and what follows it are siblings sharing one card, not a
 * parent and its children.
 */
@Composable
private fun PreviewGroup(content: @Composable () -> Unit) {
    Column(content = { content() })
}

@Composable
private fun PreviewTokenRow() {
    TangemTokenRow(
        icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
        title = stringReference("Bitcoin"),
        quote = stringReference("$1.00"),
        priceChange = TangemPriceChange.State(
            value = stringReference("2.08%"),
            direction = TangemPriceChange.Direction.Up,
        ),
        fiatBalance = stringReference("$583.00"),
        cryptoBalance = stringReference("0,000015 BTC"),
        modifier = Modifier
            .clip(PreviewClosingShape)
            .background(TangemTheme.colors3.bg.secondary),
    )
}

@Composable
private fun PreviewContainer(content: @Composable (avatar: TangemAvatar.UiState) -> Unit) {
    val avatar = TangemAvatar.UiState.Account(
        iconRes = R.drawable.ic_airplane_mode_24,
        color = TangemAvatar.FillColor.DarkBlue,
    )
    TangemThemePreviewRedesign {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        ) {
            content(avatar)
        }
    }
}

// endregion