@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.tokenrow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 (DS3) **Token Row Market** — a markets-list item: token icon, title with a
 * ticker, rank badge + capitalization, price with a [TangemPriceChange] indicator and a trailing
 * mini-graph slot.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5724-3510&m=dev)
 *
 * @param icon Token icon state, rendered at 40dp. See [TangemTokenIcon.UiState].
 * @param title Token name. Single line, ellipsized.
 * @param modifier Modifier applied to the row container.
 * @param ticker Currency ticker after the title (e.g. `"BTC"`), baseline-aligned. `null` hides it.
 * @param position Market-rank badge label (e.g. `"2"`). `null` hides the badge (Figma `Position`).
 * @param capitalization Market capitalization text (e.g. `"1.196T"`). `null` hides it.
 * @param price Current price at the end (e.g. `"$59,723.24"`). `null` hides the line.
 * @param priceChange Price change indicator under the [price]. `null` hides it.
 * @param priceUpdateDirection Direction of the latest live price update. When set, the [price]
 * text flashes in the direction color and fades back each time [price] changes. `null` disables
 * the flash.
 * @param chart Trailing mini-graph slot, vertically centered. `null` hides it.
 * @param onClick Row click handler. `null` makes the row non-interactive (no ripple/focus).
 */
@Suppress("LongParameterList")
@Composable
fun TangemTokenRowMarket(
    icon: TangemTokenIcon.UiState,
    title: TextReference,
    modifier: Modifier = Modifier,
    ticker: TextReference? = null,
    position: TextReference? = null,
    capitalization: TextReference? = null,
    price: TextReference? = null,
    priceChange: TangemPriceChange.State? = null,
    priceUpdateDirection: TangemPriceChange.Direction? = null,
    chart: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    TokenRowContainer(
        modifier = modifier,
        onClick = onClick,
    ) {
        TangemTokenIcon(
            state = icon,
            size = TangemTokenIcon.Size.X40,
            modifier = Modifier.layoutId(layoutId = TokenRowLayoutId.HEAD),
        )
        TokenRowTitleContent(
            title = title,
            ticker = ticker,
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.START_TOP)
                .padding(start = 12.dp),
        )
        if (position != null || capitalization != null) {
            TokenRowMarketSubtitleContent(
                position = position,
                capitalization = capitalization,
                modifier = Modifier
                    .layoutId(layoutId = TokenRowLayoutId.START_BOTTOM)
                    .padding(start = 12.dp),
            )
        }
        if (price != null) {
            TokenRowMarketPriceContent(
                price = price,
                updateDirection = priceUpdateDirection,
                modifier = Modifier
                    .layoutId(layoutId = TokenRowLayoutId.END_TOP)
                    .padding(start = 12.dp),
            )
        }
        if (priceChange != null) {
            TangemPriceChange(
                state = priceChange,
                modifier = Modifier
                    .layoutId(layoutId = TokenRowLayoutId.END_BOTTOM)
                    .padding(start = 12.dp),
            )
        }
        if (chart != null) {
            Box(
                modifier = Modifier
                    .layoutId(layoutId = TokenRowLayoutId.TAIL)
                    .padding(start = 12.dp),
            ) {
                chart()
            }
        }
    }
}

/**
 * Design-system v2 (DS3) **Token Row Market** — state-driven overload: renders the variant
 * described by [TangemTokenRowMarket.State].
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5724-3510&m=dev)
 *
 * @param state Row state model. See [TangemTokenRowMarket.State].
 * @param modifier Modifier applied to the row container.
 * @param chart Trailing mini-graph slot; only used by [TangemTokenRowMarket.State.Content].
 * Stays a slot (not a state field) because the chart component lives in `:common:ui-charts`.
 */
@Composable
fun TangemTokenRowMarket(
    state: TangemTokenRowMarket.State,
    modifier: Modifier = Modifier,
    chart: (@Composable () -> Unit)? = null,
) {
    when (state) {
        is TangemTokenRowMarket.State.Content -> TangemTokenRowMarket(
            icon = state.icon,
            title = state.title,
            modifier = modifier,
            ticker = state.ticker,
            position = state.position,
            capitalization = state.capitalization,
            price = state.price,
            priceChange = state.priceChange,
            priceUpdateDirection = state.priceUpdateDirection,
            chart = chart,
            onClick = state.onClick,
        )
        is TangemTokenRowMarket.State.Shimmer -> TangemTokenRowMarket.Shimmer(modifier = modifier)
    }
}

/** Public API surface of [TangemTokenRowMarket]. */
object TangemTokenRowMarket {

    /**
     * State model of [TangemTokenRowMarket]. Render it with the `TangemTokenRowMarket(state = …)`
     * overload.
     */
    @Immutable
    sealed class State {

        /** Unique id, e.g. for `LazyColumn` item keys. */
        abstract val id: String

        /**
         * Loaded market row.
         *
         * @param id Unique id.
         * @param icon Token icon state.
         * @param title Token name.
         * @param ticker Currency ticker after the title. `null` hides it.
         * @param position Market-rank badge label. `null` hides the badge.
         * @param capitalization Market capitalization text. `null` hides it.
         * @param price Current price at the end. `null` hides the line.
         * @param priceChange Price change indicator under the price. `null` hides it.
         * @param priceUpdateDirection Direction of the latest live price update — the price text
         * flashes in this color each time [price] changes. `null` disables the flash.
         * @param onClick Row click handler. `null` makes the row non-interactive.
         */
        data class Content(
            override val id: String,
            val icon: TangemTokenIcon.UiState,
            val title: TextReference,
            val ticker: TextReference? = null,
            val position: TextReference? = null,
            val capitalization: TextReference? = null,
            val price: TextReference? = null,
            val priceChange: TangemPriceChange.State? = null,
            val priceUpdateDirection: TangemPriceChange.Direction? = null,
            val onClick: (() -> Unit)? = null,
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
 * Design-system v2 (DS3) **Token Row Market / Shimmer** — loading placeholder: circular icon
 * shimmer, text-line bars on both sides and a graph-sized bar at the end.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5724-3510&m=dev)
 *
 * @param modifier Modifier applied to the row container.
 */
@Composable
fun TangemTokenRowMarket.Shimmer(modifier: Modifier = Modifier) {
    TokenRowContainer(modifier = modifier) {
        TangemTokenIcon(
            state = TangemTokenIcon.UiState.Shimmer,
            size = TangemTokenIcon.Size.X40,
            modifier = Modifier.layoutId(layoutId = TokenRowLayoutId.HEAD),
        )
        TokenRowShimmerLine(
            style = TangemTheme.typography3.body.medium,
            width = 72.dp,
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.START_TOP)
                .padding(start = 12.dp),
        )
        TokenRowShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 44.dp,
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.START_BOTTOM)
                .padding(start = 12.dp),
        )
        TokenRowShimmerLine(
            style = TangemTheme.typography3.body.medium,
            width = 72.dp,
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.END_TOP)
                .padding(start = 12.dp),
        )
        TokenRowShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 44.dp,
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.END_BOTTOM)
                .padding(start = 12.dp),
        )
        // Graph placeholder — a small bar centered in the 24x32 graph slot.
        Box(
            modifier = Modifier
                .layoutId(layoutId = TokenRowLayoutId.TAIL)
                .padding(start = 12.dp)
                .size(width = 24.dp, height = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            TangemShimmer(
                radius = 4.dp,
                modifier = Modifier.size(width = 24.dp, height = 12.dp),
            )
        }
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
private fun TangemTokenRowMarketPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            TangemTokenRowMarket(
                icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
                title = stringReference("Bitcoin"),
                ticker = stringReference("BTC"),
                position = stringReference("2"),
                capitalization = stringReference("1.196T"),
                price = stringReference("$59,723.24"),
                priceChange = TangemPriceChange.State(
                    value = stringReference("2.08%"),
                    direction = TangemPriceChange.Direction.Up,
                ),
                chart = { PreviewChartPlaceholder() },
                onClick = {},
            )
            TangemTokenRowMarket(
                icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
                title = stringReference("Very Long Token Name Coin"),
                ticker = stringReference("VLTNC"),
                capitalization = stringReference("796.9B"),
                price = stringReference("$2,591.65"),
                priceChange = TangemPriceChange.State(
                    value = stringReference("0.42%"),
                    direction = TangemPriceChange.Direction.Down,
                ),
                onClick = {},
            )
            TangemTokenRowMarket.Shimmer()
        }
    }
}

@Composable
private fun PreviewChartPlaceholder() {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 32.dp)
            .background(TangemTheme.colors3.bg.tertiary),
    )
}

// endregion