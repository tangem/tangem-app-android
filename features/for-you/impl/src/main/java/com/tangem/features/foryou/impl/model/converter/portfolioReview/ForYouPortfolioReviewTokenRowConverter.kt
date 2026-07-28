package com.tangem.features.foryou.impl.model.converter.portfolioReview

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.model.converter.forYouPlaceholderBadge
import com.tangem.features.foryou.impl.model.converter.toForYouPercent
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/**
 * Builds a single per-network child row of an asset for the For You portfolio review.
 *
 * The input is all [CryptoCurrencyStatus]es of one asset on the *same* network (the asset may be held in
 * several accounts on that network). They are aggregated into one row — the crypto amount and fiat balance
 * are the per-network totals — so a network never appears twice within an asset's expanded breakdown.
 *
 * 1. all [CryptoCurrencyStatus.Loading] → a Loading row;
 * 2. any [CryptoCurrencyStatus.MissedDerivation] → "no address" treatment (missing address dominates —
 *    the balance for that portion can't be trusted);
 * 3. any [CryptoCurrencyStatus.Unreachable] / [CryptoCurrencyStatus.NoAmount] → "unreachable" treatment;
 * 4. otherwise a normal content row summing the loaded/custom/no-quote/no-account amounts.
 *
 * [CryptoCurrencyStatus.Loading] entries inside an otherwise-resolved group are ignored for
 * classification (they contribute nothing yet). The cache/flicker indicators derive from the most
 * conservative [CryptoCurrencyStatus.Sources.total] across the contributing statuses.
 */
internal class ForYouPortfolioReviewTokenRowConverter(
    private val appCurrency: AppCurrency,
    private val userWalletId: UserWalletId?,
    private val totalFiatBalance: BigDecimal,
    private val onTokenClick: (UserWalletId, CryptoCurrency) -> Unit,
) : Converter<List<CryptoCurrencyStatus>, TangemTokenRowUM> {

    private val iconConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: List<CryptoCurrencyStatus>): TangemTokenRowUM {
        val representative = value.first()
        if (value.all { it.value is CryptoCurrencyStatus.Loading }) {
            return TangemTokenRowUM.Loading(id = representative.currency.id.value)
        }

        val currency = representative.currency
        val cryptoAmount = value.sumOf { it.value.amount.orZero() }
        val fiatAmount = value.sumOf { it.value.fiatAmount.orZero() }
        val state = value.classify()

        return TangemTokenRowUM.Content(
            id = currency.id.value,
            headIconUM = TangemIconUM.Currency(iconConverter.convert(representative)),
            titleUM = toRowTitle(currency),
            subtitleUM = toRowSubtitle(state, currency, cryptoAmount),
            topEndContentUM = toRowTopEnd(state, fiatAmount),
            bottomEndContentUM = toRowBottomEnd(state, fiatAmount),
            onItemClick = { if (userWalletId != null) onTokenClick(userWalletId, currency) },
            onItemLongClick = null,
        )
    }

    /**
     * Maps the aggregate status of [statuses] onto a row's top/bottom end content, rendering the given
     * pre-summed [fiatAmount]. Reflects the same cache-flicker / could-not-refresh / no-address /
     * unreachable treatment as [convert], so the asset-level row surfaces the combined status
     * of its holdings — analogous to how `AccountCryptoPortfolioItemStateConverter` reflects a
     * `TotalFiatBalance`'s status on the account row.
     *
     * Callers must handle the all-[CryptoCurrencyStatus.Loading] case (a Loading row) before calling this.
     */
    fun toEndContent(statuses: List<CryptoCurrencyStatus>, fiatAmount: BigDecimal): EndContent {
        val state = statuses.classify()
        return EndContent(
            top = toRowTopEnd(state, fiatAmount),
            bottom = toRowBottomEnd(state, fiatAmount),
        )
    }

    /** Title: For You always shows the asset name with the placeholder price-change badge. */
    private fun toRowTitle(currency: CryptoCurrency): TangemTokenRowUM.TitleUM = TangemTokenRowUM.TitleUM.Content(
        text = stringReference(currency.name),
        badge = forYouPlaceholderBadge(),
    )

    /**
     * Subtitle: `network • amount` for resolved states, error messaging otherwise. Kept single-line to
     * match For You's style (no separate price-change line as in the wallet).
     */
    private fun toRowSubtitle(
        state: RowState,
        currency: CryptoCurrency,
        cryptoAmount: BigDecimal,
    ): TangemTokenRowUM.SubtitleUM = when (state) {
        is RowState.Normal -> TangemTokenRowUM.SubtitleUM.Content(
            text = stringReference(
                "${currency.network.name} ${StringsSigns.DOT} ${
                    cryptoAmount.format {
                        crypto(
                            cryptoCurrency = currency,
                        )
                    }
                }",
            ),
            isFlickering = state.isFlickering,
        )
        RowState.NoAddress -> TangemTokenRowUM.SubtitleUM.Content(
            text = stringReference("${currency.network.name} ${StringsSigns.DOT} ${StringsSigns.DASH_SIGN}"),
        )
        RowState.Unreachable -> TangemTokenRowUM.SubtitleUM.Content(
            text = stringReference(currency.network.name),
        )
    }

    /** Top-end: fiat total for resolved states, dash / unreachable treatment otherwise. */
    private fun toRowTopEnd(state: RowState, fiatAmount: BigDecimal): TangemTokenRowUM.EndContentUM = when (state) {
        is RowState.Normal -> TangemTokenRowUM.EndContentUM.Content(
            text = stringReference(
                fiatAmount.format {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    )
                },
            ),
            isFlickering = state.isFlickering,
            startIcons = buildList {
                if (state.isOnlyCache) {
                    add(
                        TangemIconUM.Icon(
                            iconRes = R.drawable.ic_error_sync_default_24,
                            tintReference = { TangemTheme.colors3.icon.tertiary },
                        ),
                    )
                }
            }.toImmutableList(),
        )
        RowState.NoAddress,
        RowState.Unreachable,
        -> TangemTokenRowUM.EndContentUM.Content(text = stringReference(StringsSigns.DASH_SIGN))
    }

    /** Bottom-end: percentage share for resolved states, no-address / unreachable treatment otherwise. */
    private fun toRowBottomEnd(state: RowState, fiatAmount: BigDecimal): TangemTokenRowUM.EndContentUM = when (state) {
        is RowState.Normal -> TangemTokenRowUM.EndContentUM.Content(
            text = stringReference(fiatAmount.toForYouPercent(totalFiatBalance).orZero().format { percent() }),
            isFlickering = state.isFlickering,
        )
        RowState.NoAddress -> attentionEndContent(R.string.common_no_address)
        RowState.Unreachable -> attentionEndContent(R.string.common_unreachable)
    }

    private fun attentionEndContent(textRes: Int): TangemTokenRowUM.EndContentUM =
        TangemTokenRowUM.EndContentUM.Content(
            text = styledResourceReference(
                id = textRes,
                spanStyleReference = { SpanStyle(color = TangemTheme.colors3.text.status.warning) },
            ),
            endIcons = persistentListOf(
                TangemIconUM.Icon(
                    iconRes = R.drawable.ic_attention_default_24,
                    tintReference = { TangemTheme.colors3.icon.status.warning },
                ),
            ),
        )

    /**
     * Collapses a mixed group into a single [RowState]. Loading-only groups are handled earlier, so a
     * group reaching here has at least one non-loading status. See the class KDoc for the priority rule.
     */
    private fun List<CryptoCurrencyStatus>.classify(): RowState {
        val resolved = filterNot { it.value is CryptoCurrencyStatus.Loading }.map { it.value }
        return when {
            resolved.any { it is CryptoCurrencyStatus.MissedDerivation } -> RowState.NoAddress
            resolved.any {
                it is CryptoCurrencyStatus.Unreachable || it is CryptoCurrencyStatus.NoAmount
            } -> RowState.Unreachable
            else -> {
                val worstSource = resolved.map { it.sources.total }.worst()
                RowState.Normal(
                    isFlickering = worstSource == StatusSource.CACHE,
                    isOnlyCache = worstSource == StatusSource.ONLY_CACHE,
                )
            }
        }
    }

    /**
     * The most conservative status across the group: any [StatusSource.ONLY_CACHE] (could-not-refresh)
     * dominates a [StatusSource.CACHE] (still refreshing), which in turn dominates [StatusSource.ACTUAL].
     */
    private fun List<StatusSource>.worst(): StatusSource = when {
        any { it == StatusSource.ONLY_CACHE } -> StatusSource.ONLY_CACHE
        any { it == StatusSource.CACHE } -> StatusSource.CACHE
        else -> StatusSource.ACTUAL
    }

    /** The top and bottom end content of a token row, produced together from one classified group. */
    data class EndContent(
        val top: TangemTokenRowUM.EndContentUM,
        val bottom: TangemTokenRowUM.EndContentUM,
    )

    /** Rendering-relevant collapse of the group's per-currency-status states. */
    private sealed interface RowState {
        /** Loaded / Custom / NoQuote / NoAccount — normal amounts, with cache/flicker indicators. */
        data class Normal(val isFlickering: Boolean, val isOnlyCache: Boolean) : RowState

        /** At least one MissedDerivation — no blockchain address obtained. */
        data object NoAddress : RowState

        /** At least one Unreachable / NoAmount — network could not be reached. */
        data object Unreachable : RowState
    }
}