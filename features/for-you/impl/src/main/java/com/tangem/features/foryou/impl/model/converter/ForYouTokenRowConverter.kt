package com.tangem.features.foryou.impl.model.converter

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

/**
 * Builds a single per-network child row of an asset for the For You portfolio review.
 *
 * The input is all [CryptoCurrencyStatus]es of one asset on the *same* network (the asset may be held in
 * several accounts on that network). They are aggregated into one row — the crypto amount and fiat balance
 * are the per-network totals — so a network never appears twice within an asset's expanded breakdown.
 */
internal class ForYouTokenRowConverter(
    private val appCurrency: AppCurrency,
    private val totalFiatBalance: BigDecimal,
) {

    private val iconConverter = CryptoCurrencyToIconStateConverter()

    /** Builds one row for all [statuses] of a single asset on the same network. */
    fun convertNetworkGroup(statuses: List<CryptoCurrencyStatus>): TangemTokenRowUM {
        val representative = statuses.first()
        if (statuses.all { it.value is CryptoCurrencyStatus.Loading }) {
            return TangemTokenRowUM.Loading(id = representative.currency.id.value)
        }

        val currency = representative.currency
        val cryptoAmount = statuses.sumOf { it.value.amount.orZero() }
        val fiatAmount = statuses.sumOf { it.value.fiatAmount.orZero() }
        return TangemTokenRowUM.Content(
            id = currency.id.value,
            headIconUM = TangemIconUM.Currency(iconConverter.convert(representative)),
            titleUM = TangemTokenRowUM.TitleUM.Content(
                text = stringReference(currency.symbol),
                badge = forYouPlaceholderBadge(),
            ),
            subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                text = stringReference(
                    "${currency.network.name} ${StringsSigns.DOT} ${cryptoAmount.format { crypto(
                        cryptoCurrency = currency,
                    ) }}",
                ),
            ),
            topEndContentUM = TangemTokenRowUM.EndContentUM.Content(text = fiatAmount.toForYouFiatText(appCurrency)),
            bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                text = fiatAmount.toForYouPercentText(totalFiatBalance),
            ),
            onItemClick = null,
            onItemLongClick = null,
        )
    }
}