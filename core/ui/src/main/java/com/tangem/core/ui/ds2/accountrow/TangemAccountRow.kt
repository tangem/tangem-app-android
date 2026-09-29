@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.accountrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.AccountResIcon
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
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
 * shrinks that line into a compact heading and reveals [content], normally the account's
 * [TangemTokenRow]s.
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
 * @param isExpanded Whether the card shows [content].
 * @param collapseContentDescription Accessibility label of the collapse arrow; supply it when the
 * row is expandable so the affordance is announced.
 * @param onClick Card click handler. `null` with no [onLongClick] makes the card non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 * @param content Expanded body — the account's token rows. `null` makes the card non-expandable.
 * @param avatar Leading account avatar. Render it at [AccountIconSize.Default] (40dp) and do not
 * animate it — the row shrinks it itself while expanding. `AccountResIcon` / `AccountCharIcon` /
 * `PaymentAccountIcon` all fit.
 */
@Suppress("LongParameterList")
@Composable
fun TangemAccountRow(
    title: TextReference,
    modifier: Modifier = Modifier,
    tokensAmount: TextReference? = null,
    membersAmount: TextReference? = null,
    balance: TextReference? = null,
    priceChange: TangemPriceChange.State? = null,
    showUpdateWarning: Boolean = false,
    isBalanceHidden: Boolean = false,
    isBalanceFlickering: Boolean = false,
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    avatar: @Composable () -> Unit,
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
        isExpanded = isExpanded,
        collapseContentDescription = collapseContentDescription,
        onClick = onClick,
        onLongClick = onLongClick,
        content = content,
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
         * Empty variant — an account with no tokens yet. Expanded, the balance drops out of the
         * heading and the body is replaced by the prompt to add the first ones.
         *
         * @param id Unique id.
         * @param title Account name.
         * @param message Localized prompt shown in the expanded body.
         * @param addTokensLabel Localized label of the button under [message].
         * @param onAddTokensClick Invoked when that button is tapped.
         * @param tokensAmount Localized token count (e.g. `"0 tokens"`). `null` hides it.
         * @param membersAmount Localized member count. `null` hides it and the separating dot.
         * @param balance Total fiat balance (e.g. `"$0.00"`), shown while collapsed only. `null`
         * hides it.
         * @param priceChange Price change under the collapsed balance — normally
         * [TangemPriceChange.Direction.Neutral]. `null` hides it.
         * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
         * @param onLongClick Card long-press handler. `null` disables long-press.
         */
        data class Empty(
            override val id: String,
            val title: TextReference,
            val message: TextReference,
            val addTokensLabel: TextReference,
            val onAddTokensClick: () -> Unit,
            val tokensAmount: TextReference? = null,
            val membersAmount: TextReference? = null,
            val balance: TextReference? = null,
            val priceChange: TangemPriceChange.State? = null,
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
 * @param state Card state model. See [TangemAccountRow.State].
 * @param modifier Modifier applied to the card.
 * @param isExpanded Whether the card shows [content].
 * @param isBalanceHidden When `true`, the balance is masked with stars. Kept outside [state] because
 * it is an app-wide setting, as on [TangemTokenRow].
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param content Expanded body — the account's token rows. Ignored by
 * [TangemAccountRow.State.Empty], which renders its own body, and by
 * [TangemAccountRow.State.Shimmer].
 * @param avatar Leading account avatar, rendered at [AccountIconSize.Default]. It stays outside
 * [state] because it is a composable slot.
 */
@Suppress("LongParameterList")
@Composable
fun TangemAccountRow(
    state: TangemAccountRow.State,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    isBalanceHidden: Boolean = false,
    collapseContentDescription: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    avatar: @Composable () -> Unit,
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
            content = content,
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
            content = content,
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
            content = content,
        )
        is TangemAccountRow.State.Empty -> TangemAccountRow.Empty(
            avatar = avatar,
            title = state.title,
            message = state.message,
            addTokensLabel = state.addTokensLabel,
            onAddTokensClick = state.onAddTokensClick,
            modifier = modifier,
            tokensAmount = state.tokensAmount,
            membersAmount = state.membersAmount,
            balance = state.balance,
            priceChange = state.priceChange,
            isBalanceHidden = isBalanceHidden,
            isExpanded = isExpanded,
            collapseContentDescription = collapseContentDescription,
            onClick = state.onClick,
            onLongClick = state.onLongClick,
        )
        is TangemAccountRow.State.Shimmer -> TangemAccountRow.Shimmer(modifier = modifier)
    }
}

/**
 * Design-system v2 (DS3) **Account Row / Unreachable** — the account's networks are unreachable, so
 * a warning badge replaces the balance and the price change.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10694-21239)
 *
 * @param title Account name.
 * @param badge Localized badge label (e.g. `"Unreachable"`).
 * @param modifier Modifier applied to the card.
 * @param tokensAmount Localized token count. `null` hides it.
 * @param membersAmount Localized member count. `null` hides it and the separating dot.
 * @param isExpanded Whether the card shows [content].
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 * @param content Expanded body — the account's token rows, themselves usually unreachable.
 * @param avatar Leading account avatar, rendered at [AccountIconSize.Default].
 */
@Suppress("LongParameterList")
@Composable
fun TangemAccountRow.Unreachable(
    title: TextReference,
    badge: TextReference,
    modifier: Modifier = Modifier,
    tokensAmount: TextReference? = null,
    membersAmount: TextReference? = null,
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    avatar: @Composable () -> Unit,
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
        content = content,
    )
}

/**
 * Design-system v2 (DS3) **Account Row / Balance Error** — the total could not be calculated, so an
 * em dash stands in for it. The account itself is reachable and the row stays interactive.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10694-21239)
 *
 * @param title Account name.
 * @param modifier Modifier applied to the card.
 * @param tokensAmount Localized token count. `null` hides it.
 * @param membersAmount Localized member count. `null` hides it and the separating dot.
 * @param isExpanded Whether the card shows [content].
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 * @param content Expanded body — the account's token rows.
 * @param avatar Leading account avatar, rendered at [AccountIconSize.Default].
 */
@Suppress("LongParameterList")
@Composable
fun TangemAccountRow.BalanceError(
    title: TextReference,
    modifier: Modifier = Modifier,
    tokensAmount: TextReference? = null,
    membersAmount: TextReference? = null,
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    avatar: @Composable () -> Unit,
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
        content = content,
    )
}

/**
 * Design-system v2 (DS3) **Account Row / Empty** — an account with no tokens. Collapsed it reads
 * like the default variant with a zero balance; expanded, the balance drops out of the heading and
 * the prompt to add the first tokens takes the place of the token list.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10694-21239)
 *
 * @param title Account name.
 * @param message Localized prompt shown in the expanded body.
 * @param addTokensLabel Localized label of the button under [message].
 * @param onAddTokensClick Invoked when that button is tapped.
 * @param modifier Modifier applied to the card.
 * @param tokensAmount Localized token count (e.g. `"0 tokens"`). `null` hides it.
 * @param membersAmount Localized member count. `null` hides it and the separating dot.
 * @param balance Total fiat balance (e.g. `"$0.00"`), shown while collapsed only — the expanded
 * heading of an empty account carries nothing but its name. `null` hides it.
 * @param priceChange Price change under the collapsed balance — normally
 * [TangemPriceChange.Direction.Neutral]. `null` hides it.
 * @param isBalanceHidden When `true`, [balance] is masked with stars.
 * @param isExpanded Whether the card shows the add-tokens body.
 * @param collapseContentDescription Accessibility label of the collapse arrow.
 * @param onClick Card click handler. `null` with no [onLongClick] makes it non-interactive.
 * @param onLongClick Card long-press handler. `null` disables long-press.
 * @param avatar Leading account avatar, rendered at [AccountIconSize.Default].
 */
@Suppress("LongParameterList")
@Composable
fun TangemAccountRow.Empty(
    title: TextReference,
    message: TextReference,
    addTokensLabel: TextReference,
    onAddTokensClick: () -> Unit,
    modifier: Modifier = Modifier,
    tokensAmount: TextReference? = null,
    membersAmount: TextReference? = null,
    balance: TextReference? = null,
    priceChange: TangemPriceChange.State? = null,
    isBalanceHidden: Boolean = false,
    isExpanded: Boolean = false,
    collapseContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    avatar: @Composable () -> Unit,
) {
    AccountRowScaffold(
        avatar = avatar,
        title = title,
        modifier = modifier,
        tokensAmount = tokensAmount,
        membersAmount = membersAmount,
        balance = balance,
        priceChange = priceChange,
        showCompactBalance = false,
        isBalanceHidden = isBalanceHidden,
        isExpanded = isExpanded,
        collapseContentDescription = collapseContentDescription,
        onClick = onClick,
        onLongClick = onLongClick,
        content = {
            AccountRowEmptyBody(
                message = message,
                addTokensLabel = addTokensLabel,
                onAddTokensClick = onAddTokensClick,
            )
        },
    )
}

/**
 * Design-system v2 (DS3) **Account Row / Shimmer** — loading placeholder in the collapsed geometry.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10694-21239)
 *
 * @param modifier Modifier applied to the card.
 */
@Composable
fun TangemAccountRow.Shimmer(modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    AccountRowContainer(
        motion = rememberAccountRowMotion(isExpanded = false),
        interactionSource = interactionSource,
        modifier = modifier,
    ) {
        TangemShimmer(
            radius = 12.dp,
            modifier = Modifier
                .layoutId(AccountRowLayoutId.AVATAR)
                .size(40.dp),
        )
        AccountRowShimmerLine(
            lineHeight = 20.dp,
            width = 72.dp,
            modifier = Modifier.layoutId(AccountRowLayoutId.TITLE),
        )
        AccountRowShimmerLine(
            lineHeight = 16.dp,
            width = 44.dp,
            modifier = Modifier.layoutId(AccountRowLayoutId.SUBTITLE),
        )
        AccountRowShimmerLine(
            lineHeight = 20.dp,
            width = 72.dp,
            modifier = Modifier.layoutId(AccountRowLayoutId.BALANCE_LARGE),
        )
        AccountRowShimmerLine(
            lineHeight = 16.dp,
            width = 44.dp,
            modifier = Modifier.layoutId(AccountRowLayoutId.PRICE_CHANGE),
        )
    }
}

/**
 * Design-system v2 (DS3) **Account Row / Section** — a caption header grouping part of an expanded
 * account's body, e.g. `Tokens` above the token rows.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=11031-3048)
 *
 * @param title Localized section title.
 * @param modifier Modifier applied to the section column.
 * @param content Rows belonging to the section, laid out without extra insets — they bring their own.
 */
@Composable
fun TangemAccountRow.Section(
    title: TextReference,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        content()
    }
}

/**
 * Shared assembly of every [TangemAccountRow] variant: composes each Figma element into the slot the
 * container's layout expects and applies the press dim plus the per-element expansion alphas.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
private fun AccountRowScaffold(
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
    content: (@Composable ColumnScope.() -> Unit)? = null,
    avatar: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressAlpha by rememberAccountRowPressAlpha(interactionSource)
    val motion = rememberAccountRowMotion(isExpanded = isExpanded && content != null)

    AccountRowContainer(
        motion = motion,
        interactionSource = interactionSource,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        val avatarScale = motion.avatarScale
        Box(
            modifier = Modifier
                .layoutId(AccountRowLayoutId.AVATAR)
                .graphicsLayer {
                    alpha = pressAlpha
                    scaleX = avatarScale
                    scaleY = avatarScale
                    transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0f)
                },
        ) {
            avatar()
        }

        AccountRowTitle(
            title = title,
            motion = motion,
            modifier = Modifier
                .layoutId(AccountRowLayoutId.TITLE)
                .graphicsLayer { alpha = pressAlpha },
        )
        AccountRowTitleAnchor(title = title, modifier = Modifier.layoutId(AccountRowLayoutId.TITLE_ANCHOR))

        if (tokensAmount != null || membersAmount != null) {
            AccountRowSubtitle(
                tokensAmount = tokensAmount,
                membersAmount = membersAmount,
                modifier = Modifier
                    .layoutId(AccountRowLayoutId.SUBTITLE)
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
                    .layoutId(AccountRowLayoutId.END_BADGE)
                    .graphicsLayer { alpha = pressAlpha * motion.largeBalanceAlpha },
            )
            hasBalanceError -> AccountRowBalanceError(
                modifier = Modifier
                    .layoutId(AccountRowLayoutId.BALANCE_LARGE)
                    .graphicsLayer { alpha = pressAlpha * motion.largeBalanceAlpha },
            )
            balance != null -> AccountRowLargeBalance(
                balance = balance,
                showUpdateWarning = showUpdateWarning,
                isBalanceHidden = isBalanceHidden,
                isFlickering = isBalanceFlickering,
                modifier = Modifier
                    .layoutId(AccountRowLayoutId.BALANCE_LARGE)
                    .graphicsLayer { alpha = pressAlpha * motion.largeBalanceAlpha },
            )
        }

        if (priceChange != null && badge == null) {
            TangemPriceChange(
                state = priceChange,
                modifier = Modifier
                    .layoutId(AccountRowLayoutId.PRICE_CHANGE)
                    .graphicsLayer { alpha = pressAlpha * motion.priceChangeAlpha },
            )
        }

        if (balance != null && showCompactBalance) {
            AccountRowDot(
                modifier = Modifier
                    .layoutId(AccountRowLayoutId.DOT)
                    .graphicsLayer { alpha = pressAlpha * motion.dotAlpha },
            )
            AccountRowSmallBalance(
                balance = balance,
                isBalanceHidden = isBalanceHidden,
                isFlickering = isBalanceFlickering,
                modifier = Modifier
                    .layoutId(AccountRowLayoutId.BALANCE_SMALL)
                    .offset(x = motion.smallBalanceOffset)
                    .graphicsLayer { alpha = pressAlpha * motion.smallBalanceAlpha },
            )
        }

        if (content != null) {
            AccountRowCollapseArrow(
                contentDescription = collapseContentDescription,
                modifier = Modifier
                    .layoutId(AccountRowLayoutId.ARROW)
                    .graphicsLayer { alpha = pressAlpha * motion.arrowAlpha },
            )
            if (isExpanded || motion.expansion > 0f) {
                Column(
                    modifier = Modifier
                        .layoutId(AccountRowLayoutId.CONTENT)
                        .graphicsLayer { alpha = motion.slotAlpha },
                    content = content,
                )
            }
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
            isExpanded = true,
            collapseContentDescription = "Collapse",
            onClick = {},
            content = { PreviewTokenRow() },
        )
        TangemAccountRow.Empty(
            avatar = avatar,
            title = stringReference("Title"),
            message = stringReference("Add tokens to begin tracking your crypto assets and transactions"),
            addTokensLabel = stringReference("Add tokens"),
            onAddTokensClick = {},
            tokensAmount = stringReference("0 tokens"),
            membersAmount = stringReference("2 members"),
            isExpanded = true,
            collapseContentDescription = "Collapse",
            onClick = {},
        )
    }
}

@Preview(name = "Section Light", showBackground = true, widthDp = 360)
@Preview(name = "Section Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemAccountRowSectionPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            TangemAccountRow.Section(title = stringReference("Section Title")) {
                PreviewTokenRow()
            }
        }
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
private fun PreviewContainer(content: @Composable (avatar: @Composable () -> Unit) -> Unit) {
    val avatar: @Composable () -> Unit = {
        AccountResIcon(
            resId = R.drawable.ic_airplane_mode_24,
            color = TangemTheme.colors3.bg.accent.blue,
            size = AccountIconSize.Default,
        )
    }
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