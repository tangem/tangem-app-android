package com.tangem.features.foryou.impl.model.converter

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

/**
 * Builds the For You portfolio-review list: groups the given currency statuses by asset across networks
 * (see [forYouGroupKey]) and maps each group to a [ForYouTokenListItemUM] — an aggregate asset row plus,
 * when the asset spans more than one network, its per-network child rows.
 *
 * The child rows are grouped by network (delegated to [ForYouTokenRowConverter]) so a network appears
 * once per asset even if the asset is held on it in several accounts;
 *
 * Modelled on `TokenListStateConverter` (a list converter delegating to a per-item converter).
 */
internal class ForYouTokenListConverter(
    private val appCurrency: AppCurrency,
    private val totalFiatBalance: BigDecimal,
    private val expandedAssetIds: Set<String>,
    private val expandClick: (assetId: String) -> Unit,
    private val otherAssets: List<Pair<List<CryptoCurrencyStatus>, BigDecimal>>,
) : Converter<List<CryptoCurrencyStatus>, ImmutableList<ForYouTokenListItemUM>> {

    private val iconConverter = CryptoCurrencyToIconStateConverter()
    private val rowConverter = ForYouTokenRowConverter(appCurrency = appCurrency, totalFiatBalance = totalFiatBalance)

    override fun convert(value: List<CryptoCurrencyStatus>): ImmutableList<ForYouTokenListItemUM> {
        val assetItems = value
            .groupBy { it.forYouGroupKey() }
            .map { (assetId, currencies) -> createListItem(assetId, currencies) }

        // Assets beyond the top ones are collapsed into a single non-expandable "Other" row at the bottom.
        return if (otherAssets.count() > 0) {
            assetItems + createOtherItem()
        } else {
            assetItems
        }.toPersistentList()
    }

    private fun createListItem(assetId: String, currencies: List<CryptoCurrencyStatus>): ForYouTokenListItemUM {
        // Group the asset's holdings by blockchain (network.id.rawId, derivation-independent) so each
        // network appears once even when the asset is held across several accounts/derivations on it,
        // summing those balances. Order by balance so the expanded breakdown reads top-down.
        val networkGroups = currencies
            .groupBy { it.currency.network.id.rawId }
            .values
            .sortedByDescending { group -> group.sumOf { it.value.fiatAmount.orZero() } }
        return ForYouTokenListItemUM(
            tokenRowUM = createAssetRow(
                assetId = assetId,
                currencies = currencies,
                networkCount = networkGroups.size,
            ),
            tokenList = networkGroups.map(rowConverter::convertNetworkGroup).toPersistentList(),
            isExpanded = assetId in expandedAssetIds,
            isExpandable = true,
        )
    }

    private fun createAssetRow(
        assetId: String,
        currencies: List<CryptoCurrencyStatus>,
        networkCount: Int,
    ): TangemTokenRowUM {
        if (currencies.all { it.value is CryptoCurrencyStatus.Loading }) {
            return TangemTokenRowUM.Loading(id = assetId)
        }

        val asset = currencies.first()
        val assetFiatBalance = currencies.sumOf { it.value.fiatAmount.orZero() }

        val endContent = rowConverter.toEndContent(statuses = currencies, fiatAmount = assetFiatBalance)

        val onlyCryptoCurrency = currencies.firstOrNull()?.currency
        val isMain = onlyCryptoCurrency is CryptoCurrency.Coin

        val subtitle = when {
            networkCount > 1 -> pluralReference(R.plurals.common_networks_count, networkCount)
            isMain -> resourceReference(R.string.common_main_network)
            onlyCryptoCurrency != null -> stringReference(onlyCryptoCurrency.network.standardType.name)
            else -> TextReference.EMPTY
        }

        return TangemTokenRowUM.Content(
            id = assetId,
            headIconUM = TangemIconUM.Currency(iconConverter.convert(asset)),
            titleUM = TangemTokenRowUM.TitleUM.Content(
                text = stringReference(asset.currency.symbol),
                badge = forYouPlaceholderBadge(),
            ),
            subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                text = subtitle,
            ),
            topEndContentUM = endContent.top,
            bottomEndContentUM = endContent.bottom,
            onItemClick = { expandClick(assetId) },
            onItemLongClick = null,
        )
    }

    private fun createOtherItem(): ForYouTokenListItemUM {
        val otherAssetsBalance = otherAssets.sumOf { (_, assetBalance) -> assetBalance }
        return ForYouTokenListItemUM(
            tokenRowUM = TangemTokenRowUM.Content(
                id = OTHER_ROW_ID,
                headIconUM = TangemIconUM.Currency(CurrencyIconState.Empty()),
                titleUM = TangemTokenRowUM.TitleUM.Content(text = resourceReference(R.string.common_other)),
                subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                    text = pluralReference(R.plurals.market_chart_assets_android, otherAssets.count()),
                ),
                topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                    text = stringReference(
                        otherAssetsBalance.format {
                            fiat(
                                fiatCurrencyCode = appCurrency.code,
                                fiatCurrencySymbol = appCurrency.symbol,
                            )
                        },
                    ),
                ),
                bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                    text = stringReference(otherAssetsBalance.toForYouPercent(totalFiatBalance).format { percent() }),
                ),
                onItemClick = null,
                onItemLongClick = null,
            ),
            tokenList = persistentListOf(),
            isExpanded = false,
            isExpandable = false,
        )
    }

    private companion object {
        const val OTHER_ROW_ID = "for_you_other_assets"
    }
}