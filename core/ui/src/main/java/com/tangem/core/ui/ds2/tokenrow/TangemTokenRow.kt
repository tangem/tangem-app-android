@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.tokenrow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chart_bar_vertical_16
import com.tangem.core.ui.res.generated.icons.ic_sign_equal_24

/**
 * Design-system v2 (DS3) **Token Row** — a portfolio list item: token icon, title with an optional
 * badge, quote + price change, fiat/crypto balances and an optional message-bubble promo line
 * underneath. DS3 replacement for the legacy `ds/row/token` `TangemTokenRow`.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5723-2473)
 *
 * @param icon Token icon state, rendered at 40dp. See [TangemTokenIcon.UiState].
 * @param title Token name. Single line, ellipsized.
 * @param modifier Modifier applied to the row container.
 * @param badge Optional [TangemTokenRow.Badge] after the title (e.g. an `"APY 5.47%"` chip; use a
 * [TangemBadge.Variant.Solid] badge for the filled look). `null` hides it.
 * @param hasPending Shows a small spinner after the title while a transaction is pending.
 * @param quote Fiat quote for one token (e.g. `"$1.00"`). `null` hides it.
 * @param priceChange Price change indicator next to the quote. `null` hides it.
 * @param fiatBalance Primary balance at the end (e.g. `"$583.00"`). `null` hides the line.
 * @param cryptoBalance Secondary balance under [fiatBalance] (e.g. `"0,000015 BTC"`). `null` hides it.
 * @param showContractWarning Shows the orange contract-error warning before [fiatBalance].
 * @param showUpdateWarning Shows the cloud update-error icon before [fiatBalance].
 * @param isBalanceHidden When `true`, [fiatBalance] and [cryptoBalance] are masked with stars.
 * @param isQuoteFlickering Runs the blade animation over [quote] and [priceChange] while the price
 * is being refreshed.
 * @param isBalanceFlickering Runs the blade animation over the balances while they are being
 * refreshed.
 * @param messageBubble Optional slot below the row content — pass a [TangemMessageBubble].
 * @param onClick Row click handler. `null` with no [onLongClick] makes the row non-interactive.
 * @param onLongClick Row long-press handler. `null` disables long-press.
 */
@Suppress("LongParameterList")
@Composable
fun TangemTokenRow(
    icon: TangemTokenIcon.UiState,
    title: TextReference,
    modifier: Modifier = Modifier,
    badge: TangemTokenRow.Badge? = null,
    hasPending: Boolean = false,
    quote: TextReference? = null,
    priceChange: TangemPriceChange.State? = null,
    fiatBalance: TextReference? = null,
    cryptoBalance: TextReference? = null,
    showContractWarning: Boolean = false,
    showUpdateWarning: Boolean = false,
    isBalanceHidden: Boolean = false,
    isQuoteFlickering: Boolean = false,
    isBalanceFlickering: Boolean = false,
    messageBubble: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    TokenRowContainer(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        TokenRowHeadIcon(icon = icon)
        TokenRowTitleContent(
            title = title,
            badge = badge,
            hasPending = hasPending,
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.START_TOP)
                .padding(end = 8.dp),
        )
        if (quote != null || priceChange != null) {
            TokenRowSubtitleContent(
                quote = quote,
                priceChange = priceChange,
                isFlickering = isQuoteFlickering,
                modifier = Modifier
                    .layoutId(layoutId = TokenRowLayoutId.START_BOTTOM)
                    .padding(end = 8.dp),
            )
        }
        if (fiatBalance != null) {
            TokenRowBalanceContent(
                fiatBalance = fiatBalance,
                showContractWarning = showContractWarning,
                showUpdateWarning = showUpdateWarning,
                isFlickering = isBalanceFlickering,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.layoutId(layoutId = TokenRowLayoutId.END_TOP),
            )
        }
        if (cryptoBalance != null) {
            TokenRowCaptionText(
                text = cryptoBalance,
                isFlickering = isBalanceFlickering,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.layoutId(layoutId = TokenRowLayoutId.END_BOTTOM),
            )
        }
        if (messageBubble != null) {
            Box(
                modifier = Modifier
                    .layoutId(layoutId = TokenRowLayoutId.EXTRA_BOTTOM)
                    .padding(start = 40.dp, bottom = 8.dp)
                    .fillMaxWidth(),
            ) {
                messageBubble()
            }
        }
    }
}

/** Public API surface of [TangemTokenRow]. */
object TangemTokenRow {

    /**
     * Badge shown after the title (e.g. an `"APY 5.47%"` chip). Wraps [TangemBadge] so callers can
     * pick a tinted or a filled ([TangemBadge.Variant.Solid]) look with a status color, as in
     * production usage.
     *
     * @param text Badge label.
     * @param variant Badge appearance — [TangemBadge.Variant.Tinted] (default) or
     * [TangemBadge.Variant.Solid] for the filled look. See [TangemBadge.Variant].
     * @param status Status color scheme. See [TangemBadge.Status].
     */
    @Immutable
    data class Badge(
        val text: TextReference,
        val variant: TangemBadge.Variant = TangemBadge.Variant.Tinted,
        val status: TangemBadge.Status = TangemBadge.Status.Neutral,
    )

    /**
     * Model of the message bubble rendered under the row content by the [State.Content] overload.
     * Mirrors the [TangemMessageBubble] parameters.
     *
     * @param text Bubble message.
     * @param variant Bubble appearance. See [TangemMessageBubble.Variant].
     * @param shouldShowTip Whether the tail on top of the bubble is drawn.
     * @param icon Leading 16dp bubble icon. `null` hides it.
     * @param onClick Invoked when the bubble body is tapped. `null` makes it non-interactive.
     * @param onClose Invoked when the bubble close button is tapped. `null` hides the button.
     * @param closeContentDescription Accessibility label for the bubble close button.
     */
    @Immutable
    data class MessageBubble(
        val text: TextReference,
        val variant: TangemMessageBubble.Variant = TangemMessageBubble.Variant.Neutral,
        val shouldShowTip: Boolean = true,
        val icon: ImageVector? = null,
        val onClick: (() -> Unit)? = null,
        val onClose: (() -> Unit)? = null,
        val closeContentDescription: String? = null,
    )

    /**
     * State model of [TangemTokenRow] — one subtype per Figma variant. Render it with the
     * `TangemTokenRow(state = …)` overload.
     */
    @Immutable
    sealed class State {

        /** Unique id, e.g. for `LazyColumn` item keys. */
        abstract val id: String

        /**
         * Default variant — balances, optional badge and an optional message bubble.
         *
         * @param id Unique id.
         * @param icon Token icon state.
         * @param title Token name.
         * @param badge Badge after the title. `null` hides it. See [Badge].
         * @param hasPending Shows a small spinner after the title while a transaction is pending.
         * @param quote Fiat quote for one token. `null` hides it.
         * @param priceChange Price change indicator next to the quote. `null` hides it.
         * @param fiatBalance Primary balance at the end. `null` hides the line.
         * @param cryptoBalance Secondary balance under [fiatBalance]. `null` hides it.
         * @param shouldShowContractWarning Shows the contract-error warning before [fiatBalance].
         * @param shouldShowUpdateWarning Shows the update-error icon before [fiatBalance].
         * @param isQuoteFlickering Runs the blade animation over the quote and price change while
         * the price is being refreshed.
         * @param isBalanceFlickering Runs the blade animation over the balances while they are
         * being refreshed.
         * @param messageBubble Message bubble under the row content. `null` hides it.
         * @param onClick Row click handler. `null` with no [onLongClick] makes the row
         * non-interactive.
         * @param onLongClick Row long-press handler. `null` disables long-press.
         */
        data class Content(
            override val id: String,
            val icon: TangemTokenIcon.UiState,
            val title: TextReference,
            val badge: Badge? = null,
            val hasPending: Boolean = false,
            val quote: TextReference? = null,
            val priceChange: TangemPriceChange.State? = null,
            val fiatBalance: TextReference? = null,
            val cryptoBalance: TextReference? = null,
            val shouldShowContractWarning: Boolean = false,
            val shouldShowUpdateWarning: Boolean = false,
            val isQuoteFlickering: Boolean = false,
            val isBalanceFlickering: Boolean = false,
            val messageBubble: MessageBubble? = null,
            val onClick: (() -> Unit)? = null,
            val onLongClick: (() -> Unit)? = null,
        ) : State()

        /**
         * Organize (reorder) variant — ticker after the title, fiat balance underneath, drag
         * handle at the end. Pass the reorder modifier via `dragHandleModifier` of the overload.
         *
         * @param id Unique id.
         * @param icon Token icon state.
         * @param title Token name.
         * @param ticker Currency ticker after the title. `null` hides it.
         * @param fiatBalance Balance line under the title. `null` hides it.
         */
        data class Organize(
            override val id: String,
            val icon: TangemTokenIcon.UiState,
            val title: TextReference,
            val ticker: TextReference? = null,
            val fiatBalance: TextReference? = null,
        ) : State()

        /**
         * Unreachable variant — dimmed leading texts and a warning badge instead of balances.
         *
         * @param id Unique id.
         * @param icon Token icon state.
         * @param title Token name, dimmed.
         * @param badge Localized badge label (e.g. `"Unreachable"`).
         * @param quote Fiat quote, dimmed. `null` hides it.
         * @param priceChange Price change indicator, dimmed. `null` hides it.
         * @param onClick Row click handler. `null` with no [onLongClick] makes the row
         * non-interactive.
         * @param onLongClick Row long-press handler. `null` disables long-press.
         */
        data class Unreachable(
            override val id: String,
            val icon: TangemTokenIcon.UiState,
            val title: TextReference,
            val badge: TextReference,
            val quote: TextReference? = null,
            val priceChange: TangemPriceChange.State? = null,
            val onClick: (() -> Unit)? = null,
            val onLongClick: (() -> Unit)? = null,
        ) : State()

        /**
         * No-address variant — a tertiary message instead of balances.
         *
         * @param id Unique id.
         * @param icon Token icon state.
         * @param title Token name.
         * @param message Localized end message (e.g. `"No address"`).
         * @param quote Fiat quote. `null` hides it.
         * @param priceChange Price change indicator. `null` hides it.
         * @param onClick Row click handler. `null` with no [onLongClick] makes the row
         * non-interactive.
         * @param onLongClick Row long-press handler. `null` disables long-press.
         */
        data class NoAddress(
            override val id: String,
            val icon: TangemTokenIcon.UiState,
            val title: TextReference,
            val message: TextReference,
            val quote: TextReference? = null,
            val priceChange: TangemPriceChange.State? = null,
            val onClick: (() -> Unit)? = null,
            val onLongClick: (() -> Unit)? = null,
        ) : State()

        /**
         * Loading variant — icon and text-line shimmers.
         *
         * @param id Unique id.
         */
        data class Shimmer(
            override val id: String,
        ) : State()
    }
}

/**
 * Design-system v2 (DS3) **Token Row** — state-driven overload: renders the variant described by
 * [TangemTokenRow.State].
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5723-2473)
 *
 * @param state Row state model. See [TangemTokenRow.State].
 * @param modifier Modifier applied to the row container.
 * @param isBalanceHidden When `true`, balances are masked with stars. Kept outside [state] because
 * it is an app-wide setting, like in the legacy DS2 row.
 * @param dragHandleModifier Modifier applied to the drag-handle icon of the
 * [TangemTokenRow.State.Organize] variant (e.g. a reorderable drag-handle modifier); ignored by
 * other variants.
 */
@Composable
fun TangemTokenRow(
    state: TangemTokenRow.State,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
) {
    when (state) {
        is TangemTokenRow.State.Content -> TangemTokenRow(
            icon = state.icon,
            title = state.title,
            modifier = modifier,
            badge = state.badge,
            hasPending = state.hasPending,
            quote = state.quote,
            priceChange = state.priceChange,
            fiatBalance = state.fiatBalance,
            cryptoBalance = state.cryptoBalance,
            showContractWarning = state.shouldShowContractWarning,
            showUpdateWarning = state.shouldShowUpdateWarning,
            isBalanceHidden = isBalanceHidden,
            isQuoteFlickering = state.isQuoteFlickering,
            isBalanceFlickering = state.isBalanceFlickering,
            messageBubble = state.messageBubble?.let { bubble -> { TokenRowMessageBubble(bubble = bubble) } },
            onClick = state.onClick,
            onLongClick = state.onLongClick,
        )
        is TangemTokenRow.State.Organize -> TangemTokenRow.Organize(
            icon = state.icon,
            title = state.title,
            modifier = modifier,
            ticker = state.ticker,
            fiatBalance = state.fiatBalance,
            isBalanceHidden = isBalanceHidden,
            dragHandleModifier = dragHandleModifier,
        )
        is TangemTokenRow.State.Unreachable -> TangemTokenRow.Unreachable(
            icon = state.icon,
            title = state.title,
            badge = state.badge,
            modifier = modifier,
            quote = state.quote,
            priceChange = state.priceChange,
            onClick = state.onClick,
            onLongClick = state.onLongClick,
        )
        is TangemTokenRow.State.NoAddress -> TangemTokenRow.NoAddress(
            icon = state.icon,
            title = state.title,
            message = state.message,
            modifier = modifier,
            quote = state.quote,
            priceChange = state.priceChange,
            onClick = state.onClick,
            onLongClick = state.onLongClick,
        )
        is TangemTokenRow.State.Shimmer -> TangemTokenRow.Shimmer(modifier = modifier)
    }
}

/** Renders the [TangemTokenRow.MessageBubble] model as a [TangemMessageBubble]. */
@Composable
private fun TokenRowMessageBubble(bubble: TangemTokenRow.MessageBubble) {
    TangemMessageBubble(
        text = bubble.text,
        variant = bubble.variant,
        showTip = bubble.shouldShowTip,
        icon = bubble.icon,
        onClick = bubble.onClick,
        onClose = bubble.onClose,
        closeContentDescription = bubble.closeContentDescription,
    )
}

/**
 * Design-system v2 (DS3) **Token Row / Organize** — reorder mode: title with a ticker, fiat balance
 * underneath and a drag handle at the end.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5723-2473)
 *
 * @param icon Token icon state, rendered at 40dp.
 * @param title Token name. Single line, ellipsized.
 * @param modifier Modifier applied to the row container.
 * @param ticker Currency ticker after the title (e.g. `"BTC"`), baseline-aligned. `null` hides it.
 * @param fiatBalance Balance line under the title (e.g. `"$583.00"`). `null` hides it.
 * @param isBalanceHidden When `true`, [fiatBalance] is masked with stars.
 * @param dragHandleModifier Modifier applied to the drag-handle icon — pass the reorderable
 * drag-handle modifier here to make the row draggable.
 */
@Composable
fun TangemTokenRow.Organize(
    icon: TangemTokenIcon.UiState,
    title: TextReference,
    modifier: Modifier = Modifier,
    ticker: TextReference? = null,
    fiatBalance: TextReference? = null,
    isBalanceHidden: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
) {
    TokenRowContainer(modifier = modifier) {
        TokenRowHeadIcon(icon = icon)
        TokenRowTitleContent(
            title = title,
            ticker = ticker,
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.START_TOP)
                .padding(end = 8.dp),
        )
        if (fiatBalance != null) {
            TokenRowCaptionText(
                text = fiatBalance,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .layoutId(layoutId = TokenRowLayoutId.START_BOTTOM)
                    .padding(end = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.TAIL)
                .padding(start = 8.dp)
                .size(24.dp)
                .then(dragHandleModifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_sign_equal_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.tertiary,
            )
        }
    }
}

/**
 * Design-system v2 (DS3) **Token Row / Unreachable** — network-error state: leading texts are
 * dimmed to the disabled opacity and a warning badge replaces the balances.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5723-2473)
 *
 * @param icon Token icon state, rendered at 40dp.
 * @param title Token name, dimmed. Single line, ellipsized.
 * @param badge Localized badge label (e.g. `"Unreachable"`), shown as a warning-tinted badge.
 * @param modifier Modifier applied to the row container.
 * @param quote Fiat quote, dimmed. `null` hides it.
 * @param priceChange Price change indicator, dimmed. `null` hides it.
 * @param onClick Row click handler. `null` with no [onLongClick] makes the row non-interactive.
 * @param onLongClick Row long-press handler. `null` disables long-press.
 */
@Suppress("LongParameterList")
@Composable
fun TangemTokenRow.Unreachable(
    icon: TangemTokenIcon.UiState,
    title: TextReference,
    badge: TextReference,
    modifier: Modifier = Modifier,
    quote: TextReference? = null,
    priceChange: TangemPriceChange.State? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    TokenRowContainer(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        TokenRowHeadIcon(icon = icon)
        TokenRowTitleContent(
            title = title,
            isDimmed = true,
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.START_TOP)
                .padding(end = 8.dp),
        )
        if (quote != null || priceChange != null) {
            TokenRowSubtitleContent(
                quote = quote,
                priceChange = priceChange,
                isDimmed = true,
                modifier = Modifier
                    .layoutId(layoutId = TokenRowLayoutId.START_BOTTOM)
                    .padding(end = 8.dp),
            )
        }
        // A single end child has no bottom counterpart, so the container centers it vertically.
        TangemBadge(
            text = badge,
            variant = TangemBadge.Variant.Tinted,
            status = TangemBadge.Status.Warning,
            size = TangemBadge.Size.X6,
            modifier = Modifier.layoutId(layoutId = TokenRowLayoutId.END_TOP),
        )
    }
}

/**
 * Design-system v2 (DS3) **Token Row / No Address** — missing-derivation state: a tertiary message
 * replaces the balances.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5723-2473)
 *
 * @param icon Token icon state, rendered at 40dp.
 * @param title Token name. Single line, ellipsized.
 * @param message Localized end message (e.g. `"No address"`), body typography in tertiary color.
 * @param modifier Modifier applied to the row container.
 * @param quote Fiat quote. `null` hides it.
 * @param priceChange Price change indicator. `null` hides it.
 * @param onClick Row click handler. `null` with no [onLongClick] makes the row non-interactive.
 * @param onLongClick Row long-press handler. `null` disables long-press.
 */
@Suppress("LongParameterList")
@Composable
fun TangemTokenRow.NoAddress(
    icon: TangemTokenIcon.UiState,
    title: TextReference,
    message: TextReference,
    modifier: Modifier = Modifier,
    quote: TextReference? = null,
    priceChange: TangemPriceChange.State? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    TokenRowContainer(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        TokenRowHeadIcon(icon = icon)
        TokenRowTitleContent(
            title = title,
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.START_TOP)
                .padding(end = 8.dp),
        )
        if (quote != null || priceChange != null) {
            TokenRowSubtitleContent(
                quote = quote,
                priceChange = priceChange,
                modifier = Modifier
                    .layoutId(layoutId = TokenRowLayoutId.START_BOTTOM)
                    .padding(end = 8.dp),
            )
        }
        // A single end child has no bottom counterpart, so the container centers it vertically.
        Text(
            text = message.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.tertiary,
            maxLines = 1,
            modifier = Modifier.layoutId(layoutId = TokenRowLayoutId.END_TOP),
        )
    }
}

/**
 * Design-system v2 (DS3) **Token Row / Shimmer** — loading placeholder: circular icon shimmer plus
 * text-line bars on both sides.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5723-2473)
 *
 * @param modifier Modifier applied to the row container.
 */
@Composable
fun TangemTokenRow.Shimmer(modifier: Modifier = Modifier) {
    TokenRowContainer(modifier = modifier) {
        TokenRowHeadIcon(icon = TangemTokenIcon.UiState.Shimmer)
        TokenRowShimmerLine(
            style = TangemTheme.typography3.body.medium,
            width = 72.dp,
            modifier = Modifier.layoutId(layoutId = TokenRowLayoutId.START_TOP),
        )
        TokenRowShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 44.dp,
            modifier = Modifier.layoutId(layoutId = TokenRowLayoutId.START_BOTTOM),
        )
        TokenRowShimmerLine(
            style = TangemTheme.typography3.body.medium,
            width = 72.dp,
            modifier = Modifier.layoutId(layoutId = TokenRowLayoutId.END_TOP),
        )
        TokenRowShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 44.dp,
            modifier = Modifier.layoutId(layoutId = TokenRowLayoutId.END_BOTTOM),
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TangemTokenRowPreview() {
    PreviewContainer { icon ->
        TangemTokenRow(
            icon = icon,
            title = stringReference("Bitcoin"),
            badge = TangemTokenRow.Badge(
                text = stringReference("APY 5.47%"),
                variant = TangemBadge.Variant.Solid,
                status = TangemBadge.Status.Info,
            ),
            quote = stringReference("$1.00"),
            priceChange = TangemPriceChange.State(
                value = stringReference("2.08%"),
                direction = TangemPriceChange.Direction.Up,
            ),
            fiatBalance = stringReference("$583.00"),
            cryptoBalance = stringReference("0,000015 BTC"),
            showContractWarning = true,
            showUpdateWarning = true,
            messageBubble = {
                TangemMessageBubble(
                    text = stringReference("Enable 5.47% APY on your balance"),
                    variant = TangemMessageBubble.Variant.Success,
                    icon = Icons.ic_chart_bar_vertical_16,
                    onClick = {},
                    onClose = {},
                    closeContentDescription = "Dismiss",
                )
            },
            onClick = {},
        )
        TangemTokenRow(
            icon = icon,
            title = stringReference("Bitcoin"),
            quote = stringReference("$1.00"),
            priceChange = TangemPriceChange.State(
                value = stringReference("0.4%"),
                direction = TangemPriceChange.Direction.Down,
            ),
            fiatBalance = stringReference("$583.00"),
            cryptoBalance = stringReference("0,000015 BTC"),
            onClick = {},
        )
        TangemTokenRow.Organize(
            icon = icon,
            title = stringReference("Bitcoin"),
            ticker = stringReference("BTC"),
            fiatBalance = stringReference("$583.00"),
        )
    }
}

@Preview(name = "States Light", showBackground = true, widthDp = 360)
@Preview(
    name = "States Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TangemTokenRowStatesPreview() {
    PreviewContainer { icon ->
        TangemTokenRow.Unreachable(
            icon = icon,
            title = stringReference("Bitcoin"),
            badge = stringReference("Unreachable"),
            quote = stringReference("$1.00"),
            priceChange = TangemPriceChange.State(
                value = stringReference("2.08%"),
                direction = TangemPriceChange.Direction.Up,
            ),
        )
        TangemTokenRow.NoAddress(
            icon = icon,
            title = stringReference("Bitcoin"),
            message = stringReference("No address"),
            quote = stringReference("$1.00"),
            priceChange = TangemPriceChange.State(
                value = stringReference("2.08%"),
                direction = TangemPriceChange.Direction.Neutral,
            ),
        )
        TangemTokenRow.Shimmer()
    }
}

@Composable
private fun PreviewContainer(content: @Composable (icon: TangemTokenIcon.UiState) -> Unit) {
    val icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null))
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            content(icon)
        }
    }
}

// endregion