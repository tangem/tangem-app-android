@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.expandablerow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * shrinks that line into a compact heading to reveal [content] when expanded.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10496-1617)
 *
 * Behavior notes:
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
 * @param isExpanded Whether the card shows [content].
 * @param collapseContentDescription Accessibility label of the collapse arrow; supply it when the
 * row is expandable so the affordance is announced.
 * @param onClick Card click handler. `null` with no [onLongClick] makes the card non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 * @param content Expanded body. `null` makes the card non-expandable.
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
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    ExpandableRowScaffold(
        avatar = avatar,
        title = title,
        modifier = modifier,
        badge = badge,
        subtitle = subtitle,
        value = value,
        caption = caption,
        isExpanded = isExpanded,
        collapseContentDescription = collapseContentDescription,
        onClick = onClick,
        onLongClick = onLongClick,
        content = content,
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
         * Empty variant — nothing to reveal yet, so the expanded body is replaced by a prompt to add
         * the first items.
         *
         * @param id Unique id.
         * @param title Row title.
         * @param message Localized prompt shown in the expanded body.
         * @param addTokensLabel Localized label of the button under [message].
         * @param onAddTokensClick Invoked when that button is tapped.
         * @param badge Localized badge label beside the title. `null` hides it.
         * @param subtitle Secondary line under the title. `null` hides it.
         * @param value Primary value at the end. `null` hides it and its separator dot.
         * @param caption Secondary line under [value]. `null` hides it and its separator dot.
         * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
         * @param onLongClick Card long-press handler. `null` disables long-press.
         */
        data class Empty(
            override val id: String,
            val title: TextReference,
            val message: TextReference,
            val addTokensLabel: TextReference,
            val onAddTokensClick: () -> Unit,
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
 * @param isExpanded Whether the card shows [content].
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param content Expanded body. Ignored by [TangemExpandableRow.State.Empty], which renders its own
 * body, and by [TangemExpandableRow.State.Shimmer].
 */
@Composable
fun TangemExpandableRow(
    avatar: TangemAvatar.UiState,
    state: TangemExpandableRow.State,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
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
            isExpanded = isExpanded,
            collapseContentDescription = collapseContentDescription,
            onClick = state.onClick,
            onLongClick = state.onLongClick,
            content = content,
        )
        is TangemExpandableRow.State.Empty -> TangemExpandableRow.Empty(
            avatar = avatar,
            title = state.title,
            message = state.message,
            addTokensLabel = state.addTokensLabel,
            onAddTokensClick = state.onAddTokensClick,
            modifier = modifier,
            badge = state.badge,
            subtitle = state.subtitle,
            value = state.value,
            caption = state.caption,
            isExpanded = isExpanded,
            collapseContentDescription = collapseContentDescription,
            onClick = state.onClick,
            onLongClick = state.onLongClick,
        )
        is TangemExpandableRow.State.Shimmer -> TangemExpandableRow.Shimmer(modifier = modifier)
    }
}

/**
 * Design-system v2 (DS3) **Expandable Row / Empty** — there is nothing to reveal yet, so expanding
 * shows the prompt to add the first items instead of a body.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10496-1617)
 *
 * @param avatar Leading avatar. See [TangemAvatar.UiState].
 * @param title Row title.
 * @param message Localized prompt shown in the expanded body.
 * @param addTokensLabel Localized label of the button under [message].
 * @param onAddTokensClick Invoked when that button is tapped.
 * @param modifier Modifier applied to the card.
 * @param badge Localized badge label beside the title. `null` hides it.
 * @param subtitle Secondary line under the title. `null` hides it.
 * @param value Primary value at the end. `null` hides it and its separator dot.
 * @param caption Secondary line under [value]. `null` hides it and its separator dot.
 * @param isExpanded Whether the card shows the prompt.
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 */
@Suppress("LongParameterList")
@Composable
fun TangemExpandableRow.Empty(
    avatar: TangemAvatar.UiState,
    title: TextReference,
    message: TextReference,
    addTokensLabel: TextReference,
    onAddTokensClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: TextReference? = null,
    subtitle: TextReference? = null,
    value: TextReference? = null,
    caption: TextReference? = null,
    isExpanded: Boolean = false,
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
        isExpanded = isExpanded,
        collapseContentDescription = collapseContentDescription,
        onClick = onClick,
        onLongClick = onLongClick,
        content = {
            ExpandableEmptyBody(
                message = message,
                addTokensLabel = addTokensLabel,
                onAddTokensClick = onAddTokensClick,
            )
        },
    )
}

/**
 * Design-system v2 (DS3) **Expandable Row / Shimmer** — loading placeholder in the collapsed
 * geometry.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10496-1617)
 *
 * @param modifier Modifier applied to the card.
 */
@Composable
fun TangemExpandableRow.Shimmer(modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    ExpandableCard(
        motion = rememberExpansionMotion(isExpanded = false),
        interactionSource = interactionSource,
        modifier = modifier,
        contentGap = 12.dp,
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
 * @param isExpanded Whether the card shows [content].
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param onClick Card click handler. `null` with no [onLongClick] makes the card non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 * @param content Expanded body. `null` makes the card non-expandable.
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
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    headingSlot: @Composable () -> Unit = {},
    contentSlot: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressAlpha by rememberRowPressAlpha(interactionSource)
    val motion = rememberExpansionMotion(isExpanded = isExpanded && content != null)

    ExpandableCard(
        motion = motion,
        interactionSource = interactionSource,
        modifier = modifier,
        contentGap = 8.dp,
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

        ExpandedRowTail(
            motion = motion,
            pressAlpha = pressAlpha,
            isExpanded = isExpanded,
            collapseContentDescription = collapseContentDescription,
            content = content,
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
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressAlpha by rememberRowPressAlpha(interactionSource)
    val motion = rememberExpansionMotion(isExpanded = isExpanded && content != null)

    ExpandableCard(
        motion = motion,
        interactionSource = interactionSource,
        modifier = modifier,
        contentGap = 8.dp,
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

        ExpandedRowTail(
            motion = motion,
            pressAlpha = pressAlpha,
            isExpanded = isExpanded,
            collapseContentDescription = collapseContentDescription,
            content = content,
        )
    }
}

@Composable
private fun ExpandedRowTail(
    motion: ExpansionMotion,
    pressAlpha: Float,
    isExpanded: Boolean,
    collapseContentDescription: String?,
    content: (@Composable ColumnScope.() -> Unit)?,
) {
    if (content == null) return

    ExpandableCollapseArrow(
        contentDescription = collapseContentDescription,
        modifier = Modifier
            .layoutId(ExpandableCardLayoutId.ARROW)
            .graphicsLayer { alpha = pressAlpha * motion.arrowAlpha },
    )
    if (isExpanded || motion.expansion > 0f) {
        Column(
            modifier = Modifier
                .layoutId(ExpandableCardLayoutId.CONTENT)
                .graphicsLayer { alpha = motion.slotAlpha },
            content = content,
        )
    }
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
        TangemExpandableRow(
            avatar = avatar,
            title = stringReference("Title"),
            badge = stringReference("Label"),
            subtitle = stringReference("Subtitle"),
            value = stringReference("Value"),
            caption = stringReference("Caption"),
            isExpanded = true,
            collapseContentDescription = "Collapse",
            onClick = {},
            content = { PreviewTokenRow() },
        )
        TangemExpandableRow.Empty(
            avatar = avatar,
            title = stringReference("Title"),
            message = stringReference("Add tokens to begin tracking your crypto assets and transactions"),
            addTokensLabel = stringReference("Add tokens"),
            onAddTokensClick = {},
            value = stringReference("Value"),
            caption = stringReference("Caption"),
            isExpanded = true,
            collapseContentDescription = "Collapse",
            onClick = {},
        )
        TangemExpandableRow.Slot(
            avatar = avatar,
            isExpanded = true,
            collapseContentDescription = "Collapse",
            onClick = {},
            content = { PreviewTokenRow() },
            headingSlot = { ExpandableCaptionText(text = stringReference("Custom heading slot")) },
            contentSlot = {
                ExpandableCaptionText(text = stringReference("Content start"), modifier = Modifier.weight(1f))
                ExpandableCaptionText(text = stringReference("Content end"))
            },
        )
    }
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