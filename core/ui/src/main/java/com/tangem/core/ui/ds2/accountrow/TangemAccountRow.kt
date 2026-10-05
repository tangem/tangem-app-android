@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.accountrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.expandable.ExpandableAvatar
import com.tangem.core.ui.ds2.expandable.ExpandableCard
import com.tangem.core.ui.ds2.expandable.ExpandableCardLayoutId
import com.tangem.core.ui.ds2.expandable.ExpandableCardShape
import com.tangem.core.ui.ds2.expandable.ExpandableCollapseArrow
import com.tangem.core.ui.ds2.expandable.ExpandableDot
import com.tangem.core.ui.ds2.expandable.ExpandableEmptyBody
import com.tangem.core.ui.ds2.expandable.ExpandableShimmerLine
import com.tangem.core.ui.ds2.expandable.ExpandableTitle
import com.tangem.core.ui.ds2.expandable.ExpandableTitleAnchor
import com.tangem.core.ui.ds2.expandable.rememberExpansionMotion
import com.tangem.core.ui.ds2.expandable.rememberRowPressAlpha
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 (DS3) **Account Row** — a portfolio card for one account. Collapsed it is a
 * summary line (avatar, name, `2 tokens · 2 members`, fiat balance and price change); expanded it
 * shrinks that line into a compact heading. The account's [TangemTokenRow]s are emitted by the
 * host next to the row, not nested inside it.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10694-21239)
 *
 * Behavior notes:
 * - [isExpanded] is an input, not internal state — the host decides which account is open, and
 *   usually only one is. Toggle it from [onClick].
 * - Expanding is a single continuous morph, not a cross-fade: the avatar shrinks 40dp → 16dp and the
 *   title travels up and scales to caption size, while the balance and the price change dissolve and
 *   the compact balance, separator dot and collapse arrow fade in behind them. Every element's
 *   timing comes from the Figma motion spec.
 * - The title is always one line and ellipsized, in both states.
 * - Holding the row down dims its heading to 60 %.
 *
 * @param avatar Leading account avatar. The row renders it itself, morphing it from 40dp to 16dp as
 * it expands — pass the state, not a pre-sized composable. See [TangemAvatar.UiState].
 * @param title Account name. Single line, ellipsized.
 * @param modifier Modifier applied to the card. The card always fills the available width.
 * @param tokensAmount Localized token count (e.g. `"2 tokens"`). `null` hides it.
 * @param membersAmount Localized member count (e.g. `"2 members"`). `null` hides it and the dot
 * separating it from [tokensAmount].
 * @param balance Total fiat balance. Shown large at the end while collapsed and small next to the
 * title while expanded. `null` hides it, the separator dot with it.
 * @param priceChange Price change under the collapsed balance. `null` hides it.
 * @param showUpdateWarning Shows the cloud update-error icon before the collapsed balance.
 * @param isBalanceHidden When `true`, [balance] is masked with stars.
 * @param isBalanceFlickering Runs the blade animation over [balance] while it is being refreshed.
 * @param showCompactBalance Whether [balance] also joins the compact heading while expanded. An
 * account with no tokens passes `false` — its open heading carries nothing but its name.
 * @param isExpanded Whether the row is open. Ignored unless [isExpandable].
 * @param collapseContentDescription Accessibility label of the collapse arrow; supply it when the
 * row is expandable so the affordance is announced.
 * @param onClick Card click handler. `null` with no [onLongClick] makes the card non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 * @param isExpandable Whether the row can be opened at all. `false` hides the collapse arrow.
 * @param shape Card background and clip. Pass a shape with square bottom corners when sibling
 * items continue the card below.
 */
@Suppress("LongParameterList")
@Composable
fun TangemAccountRow(
    avatar: TangemAvatar.UiState,
    title: TextReference,
    modifier: Modifier = Modifier,
    tokensAmount: TextReference? = null,
    membersAmount: TextReference? = null,
    balance: TextReference? = null,
    priceChange: TangemPriceChange.State? = null,
    showUpdateWarning: Boolean = false,
    isBalanceHidden: Boolean = false,
    isBalanceFlickering: Boolean = false,
    showCompactBalance: Boolean = true,
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isExpandable: Boolean = false,
    shape: Shape = ExpandableCardShape,
) {
    AccountRowScaffold(
        avatar = avatar,
        title = title,
        modifier = modifier,
        tokensAmount = tokensAmount,
        membersAmount = membersAmount,
        balance = balance,
        priceChange = priceChange,
        showUpdateWarning = showUpdateWarning,
        isBalanceHidden = isBalanceHidden,
        isBalanceFlickering = isBalanceFlickering,
        showCompactBalance = showCompactBalance,
        isExpanded = isExpanded,
        collapseContentDescription = collapseContentDescription,
        onClick = onClick,
        onLongClick = onLongClick,
        isExpandable = isExpandable,
        shape = shape,
    )
}

/** Public API surface of [TangemAccountRow]. */
object TangemAccountRow {

    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /**
     * State model of [TangemAccountRow] — one subtype per Figma variant. Render it with the
     * `TangemAccountRow(state = …)` overload.
     */
    @Immutable
    sealed class State {

        /** Unique id, e.g. for `LazyColumn` item keys. */
        abstract val id: String

        /**
         * Default variant — balance and price change at the end.
         *
         * @param id Unique id.
         * @param title Account name.
         * @param tokensAmount Localized token count. `null` hides it.
         * @param membersAmount Localized member count. `null` hides it and the separating dot.
         * @param balance Total fiat balance. `null` hides it.
         * @param priceChange Price change under the collapsed balance. `null` hides it.
         * @param shouldShowUpdateWarning Shows the update-error icon before the collapsed balance.
         * @param isBalanceFlickering Runs the blade animation over the balance while it refreshes.
         * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
         * @param onLongClick Card long-press handler. `null` disables long-press.
         */
        data class Content(
            override val id: String,
            val title: TextReference,
            val tokensAmount: TextReference? = null,
            val membersAmount: TextReference? = null,
            val balance: TextReference? = null,
            val priceChange: TangemPriceChange.State? = null,
            val shouldShowUpdateWarning: Boolean = false,
            val isBalanceFlickering: Boolean = false,
            val onClick: (() -> Unit)? = null,
            val onLongClick: (() -> Unit)? = null,
        ) : State()

        /**
         * Unreachable variant — a warning badge takes the place of the balance because the
         * account's networks could not be reached.
         *
         * @param id Unique id.
         * @param title Account name.
         * @param badge Localized badge label (e.g. `"Unreachable"`).
         * @param tokensAmount Localized token count. `null` hides it.
         * @param membersAmount Localized member count. `null` hides it and the separating dot.
         * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
         * @param onLongClick Card long-press handler. `null` disables long-press.
         */
        data class Unreachable(
            override val id: String,
            val title: TextReference,
            val badge: TextReference,
            val tokensAmount: TextReference? = null,
            val membersAmount: TextReference? = null,
            val onClick: (() -> Unit)? = null,
            val onLongClick: (() -> Unit)? = null,
        ) : State()

        /**
         * Balance-error variant — an em dash stands in for a total that could not be calculated.
         * Unlike [Unreachable] the account itself is fine, so the row stays fully interactive.
         *
         * @param id Unique id.
         * @param title Account name.
         * @param tokensAmount Localized token count. `null` hides it.
         * @param membersAmount Localized member count. `null` hides it and the separating dot.
         * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
         * @param onLongClick Card long-press handler. `null` disables long-press.
         */
        data class BalanceError(
            override val id: String,
            val title: TextReference,
            val tokensAmount: TextReference? = null,
            val membersAmount: TextReference? = null,
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
 * Design-system v2 (DS3) **Account Row** — state-driven overload: renders the variant described by
 * [TangemAccountRow.State].
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10694-21239)
 *
 * @param avatar Leading account avatar. The row renders it itself, morphing it from 40dp to 16dp as
 * it expands — pass the state, not a pre-sized composable. See [TangemAvatar.UiState].
 * @param state Card state model. See [TangemAccountRow.State].
 * @param modifier Modifier applied to the card.
 * @param isExpanded Whether the row is open. Ignored unless [isExpandable].
 * @param isBalanceHidden When `true`, the balance is masked with stars. Kept outside [state] because
 * it is an app-wide setting, as on [TangemTokenRow].
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param isExpandable Whether the row can be opened at all. `false` hides the collapse arrow.
 * @param shape Card background and clip. Pass a shape with square bottom corners when sibling
 * items continue the card below.
 */
@Suppress("LongParameterList")
@Composable
fun TangemAccountRow(
    avatar: TangemAvatar.UiState,
    state: TangemAccountRow.State,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    isBalanceHidden: Boolean = false,
    collapseContentDescription: String? = null,
    isExpandable: Boolean = false,
    shape: Shape = ExpandableCardShape,
) {
    when (state) {
        is TangemAccountRow.State.Content -> TangemAccountRow(
            avatar = avatar,
            title = state.title,
            modifier = modifier,
            tokensAmount = state.tokensAmount,
            membersAmount = state.membersAmount,
            balance = state.balance,
            priceChange = state.priceChange,
            showUpdateWarning = state.shouldShowUpdateWarning,
            isBalanceHidden = isBalanceHidden,
            isBalanceFlickering = state.isBalanceFlickering,
            isExpanded = isExpanded,
            collapseContentDescription = collapseContentDescription,
            onClick = state.onClick,
            onLongClick = state.onLongClick,
            isExpandable = isExpandable,
            shape = shape,
        )
        is TangemAccountRow.State.Unreachable -> TangemAccountRow.Unreachable(
            avatar = avatar,
            title = state.title,
            badge = state.badge,
            modifier = modifier,
            tokensAmount = state.tokensAmount,
            membersAmount = state.membersAmount,
            isExpanded = isExpanded,
            collapseContentDescription = collapseContentDescription,
            onClick = state.onClick,
            onLongClick = state.onLongClick,
            isExpandable = isExpandable,
            shape = shape,
        )
        is TangemAccountRow.State.BalanceError -> TangemAccountRow.BalanceError(
            avatar = avatar,
            title = state.title,
            modifier = modifier,
            tokensAmount = state.tokensAmount,
            membersAmount = state.membersAmount,
            isExpanded = isExpanded,
            collapseContentDescription = collapseContentDescription,
            onClick = state.onClick,
            onLongClick = state.onLongClick,
            isExpandable = isExpandable,
            shape = shape,
        )
        is TangemAccountRow.State.Shimmer -> TangemAccountRow.Shimmer(
            modifier = modifier,
            shape = shape,
        )
    }
}

/**
 * Design-system v2 (DS3) **Account Row / Unreachable** — the account's networks are unreachable, so
 * a warning badge replaces the balance and the price change.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10694-21239)
 *
 * @param avatar Leading account avatar. The row renders it itself, morphing it from 40dp to 16dp as
 * it expands — pass the state, not a pre-sized composable. See [TangemAvatar.UiState].
 * @param title Account name.
 * @param badge Localized badge label (e.g. `"Unreachable"`).
 * @param modifier Modifier applied to the card.
 * @param tokensAmount Localized token count. `null` hides it.
 * @param membersAmount Localized member count. `null` hides it and the separating dot.
 * @param isExpanded Whether the row is open. Ignored unless [isExpandable].
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 * @param isExpandable Whether the row can be opened at all. `false` hides the collapse arrow.
 * @param shape Card background and clip. Pass a shape with square bottom corners when sibling
 * items continue the card below.
 */
@Suppress("LongParameterList")
@Composable
fun TangemAccountRow.Unreachable(
    avatar: TangemAvatar.UiState,
    title: TextReference,
    badge: TextReference,
    modifier: Modifier = Modifier,
    tokensAmount: TextReference? = null,
    membersAmount: TextReference? = null,
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isExpandable: Boolean = false,
    shape: Shape = ExpandableCardShape,
) {
    AccountRowScaffold(
        avatar = avatar,
        title = title,
        modifier = modifier,
        tokensAmount = tokensAmount,
        membersAmount = membersAmount,
        badge = badge,
        isExpanded = isExpanded,
        collapseContentDescription = collapseContentDescription,
        onClick = onClick,
        onLongClick = onLongClick,
        isExpandable = isExpandable,
        shape = shape,
    )
}

/**
 * Design-system v2 (DS3) **Account Row / Balance Error** — the total could not be calculated, so an
 * em dash stands in for it. The account itself is reachable and the row stays interactive.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10694-21239)
 *
 * @param avatar Leading account avatar. The row renders it itself, morphing it from 40dp to 16dp as
 * it expands — pass the state, not a pre-sized composable. See [TangemAvatar.UiState].
 * @param title Account name.
 * @param modifier Modifier applied to the card.
 * @param tokensAmount Localized token count. `null` hides it.
 * @param membersAmount Localized member count. `null` hides it and the separating dot.
 * @param isExpanded Whether the row is open. Ignored unless [isExpandable].
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 * @param isExpandable Whether the row can be opened at all. `false` hides the collapse arrow.
 * @param shape Card background and clip. Pass a shape with square bottom corners when sibling
 * items continue the card below.
 */
@Suppress("LongParameterList")
@Composable
fun TangemAccountRow.BalanceError(
    avatar: TangemAvatar.UiState,
    title: TextReference,
    modifier: Modifier = Modifier,
    tokensAmount: TextReference? = null,
    membersAmount: TextReference? = null,
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isExpandable: Boolean = false,
    shape: Shape = ExpandableCardShape,
) {
    AccountRowScaffold(
        avatar = avatar,
        title = title,
        modifier = modifier,
        tokensAmount = tokensAmount,
        membersAmount = membersAmount,
        hasBalanceError = true,
        isExpanded = isExpanded,
        collapseContentDescription = collapseContentDescription,
        onClick = onClick,
        onLongClick = onLongClick,
        isExpandable = isExpandable,
        shape = shape,
    )
}

/**
 * Design-system v2 (DS3) **Account Row / Empty body** — the prompt to add the first tokens, shown
 * under an open account that has none.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10694-21239)
 *
 * Place it as the sibling that follows the row, where the token rows would otherwise go, and give it
 * a [shape] that closes the card.
 *
 * @param message Localized prompt.
 * @param addTokensLabel Localized label of the button under [message].
 * @param onAddTokensClick Invoked when that button is tapped.
 * @param modifier Modifier applied to the body. It always fills the available width.
 * @param shape Background and clip. Defaults to the standalone 24dp card; under a row, pass a shape
 * whose top corners are square.
 */
@Composable
fun TangemAccountRow.EmptyBody(
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
 * Design-system v2 (DS3) **Account Row / Shimmer** — loading placeholder in the collapsed geometry.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10694-21239)
 *
 * @param modifier Modifier applied to the card.
 * @param shape Card background and clip.
 */
@Composable
fun TangemAccountRow.Shimmer(modifier: Modifier = Modifier, shape: Shape = ExpandableCardShape) {
    val interactionSource = remember { MutableInteractionSource() }
    ExpandableCard(
        motion = rememberExpansionMotion(isExpanded = false),
        interactionSource = interactionSource,
        modifier = modifier,
        shape = shape,
    ) {
        TangemAvatar(
            state = TangemAvatar.UiState.Shimmer(shape = TangemAvatar.UiState.Shimmer.Shape.RoundedSquare),
            size = TangemAvatar.Size.X40,
            modifier = Modifier.layoutId(ExpandableCardLayoutId.AVATAR),
        )
        ExpandableShimmerLine(
            lineHeight = 20.dp,
            width = 72.dp,
            modifier = Modifier.layoutId(ExpandableCardLayoutId.TITLE),
        )
        ExpandableShimmerLine(
            lineHeight = 16.dp,
            width = 44.dp,
            modifier = Modifier.layoutId(ExpandableCardLayoutId.SUBTITLE),
        )
        ExpandableShimmerLine(
            lineHeight = 20.dp,
            width = 72.dp,
            modifier = Modifier.layoutId(ExpandableCardLayoutId.END_TOP),
        )
        ExpandableShimmerLine(
            lineHeight = 16.dp,
            width = 44.dp,
            modifier = Modifier.layoutId(ExpandableCardLayoutId.END_BOTTOM),
        )
    }
}

/**
 * Design-system v2 (DS3) **Account Row / Section** — a caption header grouping part of an expanded
 * account's body, e.g. `Tokens` above the token rows.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=11031-3048)
 *
 * Place it as the sibling that precedes the rows it names, and give it a [shape] that continues the
 * card — both its neighbours share the same background.
 *
 * @param title Localized section title.
 * @param modifier Modifier applied to the section header. It always fills the available width.
 * @param shape Background and clip. Between a row and its rows this is normally a square-cornered
 * shape; it only rounds where the header starts or ends the card.
 */
@Composable
fun TangemAccountRow.Section(title: TextReference, modifier: Modifier = Modifier, shape: Shape = RectangleShape) {
    Text(
        text = title.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TangemTheme.colors3.bg.secondary)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

/**
 * Shared assembly of every [TangemAccountRow] variant: composes each Figma element into the slot the
 * container's layout expects and applies the press dim plus the per-element expansion alphas.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
private fun AccountRowScaffold(
    avatar: TangemAvatar.UiState,
    title: TextReference,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    tokensAmount: TextReference? = null,
    membersAmount: TextReference? = null,
    balance: TextReference? = null,
    priceChange: TangemPriceChange.State? = null,
    badge: TextReference? = null,
    hasBalanceError: Boolean = false,
    showUpdateWarning: Boolean = false,
    showCompactBalance: Boolean = true,
    isBalanceHidden: Boolean = false,
    isBalanceFlickering: Boolean = false,
    isExpandable: Boolean = false,
    shape: Shape = ExpandableCardShape,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressAlpha by rememberRowPressAlpha(interactionSource)
    val motion = rememberExpansionMotion(isExpanded = isExpanded && isExpandable)

    ExpandableCard(
        motion = motion,
        interactionSource = interactionSource,
        modifier = modifier,
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

        ExpandableTitle(
            title = title,
            motion = motion,
            modifier = Modifier
                .layoutId(ExpandableCardLayoutId.TITLE)
                .graphicsLayer { alpha = pressAlpha },
        )
        ExpandableTitleAnchor(title = title, modifier = Modifier.layoutId(ExpandableCardLayoutId.TITLE_ANCHOR))

        if (tokensAmount != null || membersAmount != null) {
            AccountRowSubtitle(
                tokensAmount = tokensAmount,
                membersAmount = membersAmount,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.SUBTITLE)
                    .graphicsLayer { alpha = pressAlpha * motion.subtitleAlpha },
            )
        }

        when {
            badge != null -> TangemBadge(
                text = badge,
                variant = TangemBadge.Variant.Tinted,
                status = TangemBadge.Status.Warning,
                size = TangemBadge.Size.X6,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.END_SINGLE)
                    .graphicsLayer { alpha = pressAlpha * motion.endTopAlpha },
            )
            hasBalanceError -> AccountRowBalanceError(
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.END_TOP)
                    .graphicsLayer { alpha = pressAlpha * motion.endTopAlpha },
            )
            balance != null -> AccountRowLargeBalance(
                balance = balance,
                showUpdateWarning = showUpdateWarning,
                isBalanceHidden = isBalanceHidden,
                isFlickering = isBalanceFlickering,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.END_TOP)
                    .graphicsLayer { alpha = pressAlpha * motion.endTopAlpha },
            )
        }

        if (priceChange != null && badge == null) {
            TangemPriceChange(
                state = priceChange,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.END_BOTTOM)
                    .graphicsLayer { alpha = pressAlpha * motion.endBottomAlpha },
            )
        }

        if (balance != null && showCompactBalance) {
            ExpandableDot(
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.INLINE_1)
                    .graphicsLayer { alpha = pressAlpha * motion.dotAlpha },
            )
            AccountRowSmallBalance(
                balance = balance,
                isBalanceHidden = isBalanceHidden,
                isFlickering = isBalanceFlickering,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.INLINE_2)
                    .offset(x = motion.compactOffset)
                    .graphicsLayer { alpha = pressAlpha * motion.compactAlpha },
            )
        }

        if (isExpandable) {
            ExpandableCollapseArrow(
                contentDescription = collapseContentDescription,
                modifier = Modifier
                    .layoutId(ExpandableCardLayoutId.ARROW)
                    .graphicsLayer { alpha = pressAlpha * motion.arrowAlpha },
            )
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemAccountRowCollapsedPreview() {
    PreviewContainer { avatar ->
        TangemAccountRow(
            avatar = avatar,
            title = stringReference("Title"),
            tokensAmount = stringReference("2 tokens"),
            membersAmount = stringReference("2 members"),
            balance = stringReference("$12,000.00"),
            priceChange = TangemPriceChange.State(
                value = stringReference("2.08%"),
                direction = TangemPriceChange.Direction.Up,
            ),
            onClick = {},
        )
        TangemAccountRow.Unreachable(
            avatar = avatar,
            title = stringReference("Title"),
            badge = stringReference("Unreachable"),
            tokensAmount = stringReference("2 tokens"),
            membersAmount = stringReference("2 members"),
        )
        TangemAccountRow.BalanceError(
            avatar = avatar,
            title = stringReference("Title"),
            tokensAmount = stringReference("2 tokens"),
            membersAmount = stringReference("2 members"),
        )
        TangemAccountRow.Shimmer()
    }
}

@Preview(name = "Expanded Light", showBackground = true, widthDp = 360)
@Preview(name = "Expanded Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemAccountRowExpandedPreview() {
    PreviewContainer { avatar ->
        PreviewGroup {
            TangemAccountRow(
                avatar = avatar,
                title = stringReference("Title"),
                tokensAmount = stringReference("2 tokens"),
                membersAmount = stringReference("2 members"),
                balance = stringReference("$12,000.00"),
                priceChange = TangemPriceChange.State(
                    value = stringReference("2.08%"),
                    direction = TangemPriceChange.Direction.Up,
                ),
                isExpandable = true,
                isExpanded = true,
                shape = PreviewOpenShape,
                collapseContentDescription = "Collapse",
                onClick = {},
            )
            PreviewTokenRow(shape = PreviewClosingShape)
        }
        PreviewGroup {
            TangemAccountRow(
                avatar = avatar,
                title = stringReference("Title"),
                tokensAmount = stringReference("0 tokens"),
                membersAmount = stringReference("2 members"),
                isExpandable = true,
                isExpanded = true,
                shape = PreviewOpenShape,
                collapseContentDescription = "Collapse",
                onClick = {},
            )
            TangemAccountRow.EmptyBody(
                message = stringReference("Add tokens to begin tracking your crypto assets and transactions"),
                addTokensLabel = stringReference("Add tokens"),
                onAddTokensClick = {},
                shape = PreviewClosingShape,
            )
        }
    }
}

@Preview(name = "Section Light", showBackground = true, widthDp = 360)
@Preview(name = "Section Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemAccountRowSectionPreview() {
    PreviewContainer { avatar ->
        PreviewGroup {
            TangemAccountRow(
                avatar = avatar,
                title = stringReference("Title"),
                tokensAmount = stringReference("2 tokens"),
                isExpandable = true,
                isExpanded = true,
                shape = PreviewOpenShape,
                collapseContentDescription = "Collapse",
                onClick = {},
            )
            TangemAccountRow.Section(title = stringReference("Section Title"))
            PreviewTokenRow(shape = PreviewClosingShape)
        }
    }
}

/** Card shape of a row that sibling items continue below it. */
private val PreviewOpenShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

/** Card shape of the item that closes the group. */
private val PreviewClosingShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)

/**
 * Stands in for the host's list: the row and what follows it are siblings sharing one card, not a
 * parent and its children.
 */
@Composable
private fun PreviewGroup(content: @Composable () -> Unit) {
    Column(content = { content() })
}

@Composable
private fun PreviewTokenRow(shape: Shape = RectangleShape) {
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
            .clip(shape)
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