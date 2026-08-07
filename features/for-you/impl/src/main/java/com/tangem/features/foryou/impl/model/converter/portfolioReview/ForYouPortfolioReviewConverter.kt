package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.converter.FOR_YOU_TOP_EARN_TOKENS_COUNT
import com.tangem.features.foryou.impl.model.converter.forYouGroupKey
import com.tangem.features.foryou.impl.model.converter.forYouPlaceholderBadge
import com.tangem.features.foryou.impl.model.converter.toForYouPercent
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

/**
 * Builds the For You portfolio-review list: groups the given currency statuses by asset across networks
 * (see [forYouGroupKey]) and maps each group to a [ForYouTokenListItemUM] — an aggregate asset row plus,
 * when the asset spans more than one network, its per-network child rows.
 *
 * The child rows are grouped by network (delegated to [ForYouPortfolioReviewTokenRowConverter]) so a network appears
 * once per asset even if the asset is held on it in several accounts;
 *
 * Modelled on `TokenListStateConverter` (a list converter delegating to a per-item converter).
 */
internal class ForYouPortfolioReviewConverter(
    private val appCurrency: AppCurrency,
    private val expandedAssetIds: Set<String>,
    private val expandClick: (assetId: String) -> Unit,
    private val onTokenClick: (UserWalletId, CryptoCurrency) -> Unit,
    private val onAddFundsClick: (UserWalletId) -> Unit,
) : Converter<AccountStatusList?, PortfolioReviewUM> {

    private val iconConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: AccountStatusList?): PortfolioReviewUM {
        val currencies = value?.flattenCurrencies().orEmpty()
        val loadedBalance = value?.totalFiatBalance as? TotalFiatBalance.Loaded
        val totalFiatBalance = loadedBalance?.amount.orZero()

        if (currencies.all { it.value.fiatAmount?.isZero() == true }) {
            return PortfolioReviewUM.Content(
                tokenList = currencies.take(FOR_YOU_TOP_EARN_TOKENS_COUNT)
                    .groupBy { it.forYouGroupKey() }
                    .map { (assetId, currencies) ->
                        createListItem(
                            userWalletId = value?.userWalletId,
                            assetId = assetId,
                            currencies = currencies,
                            totalFiatBalance = totalFiatBalance,
                        )
                    }.toPersistentList(),
                marketChartUM = MarketChartUM.NoData(
                    title = resourceReference(R.string.market_chart_no_amount),
                    donutText = resourceReference(R.string.market_chart_bubble_no_amount),
                ),
                onAddFundsClick = { value?.userWalletId?.let(onAddFundsClick) },
            )
        }

        // Drop only assets we positively know are empty — a resolved, priced zero fiat balance. Currencies
        // whose fiat we couldn't determine (unreachable / no-address / no-quote / still-loading — i.e. any
        // non-content status, which all carry a null fiatAmount) are kept so the converter can still render
        // them with the appropriate treatment instead of hiding a token the user actually holds.
        // Then aggregate the rest into assets (the same token across networks shares its forYouGroupKey)
        // and rank assets by their *summed* fiat balance.
        val rankedAssets = currencies
            .filterNot { it.value.fiatAmount?.isZero() == true }
            .groupBy { it.forYouGroupKey() }
            .map { (_, networks) -> networks to networks.sumOf { it.value.fiatAmount.orZero() } }
            .sortedByDescending { (_, assetBalance) -> assetBalance }

        // The top assets are shown individually (each flattened back to its networks so the converter can
        // regroup them by network); the remaining assets are collapsed into a single "Other" row.
        val topAssets = rankedAssets.take(TOP_HOLDINGS_COUNT)
        val otherAssets = rankedAssets.drop(TOP_HOLDINGS_COUNT)
        val topCurrencies = topAssets.flatMap { (networks, _) -> networks }

        val assetItems = topCurrencies
            .groupBy { it.forYouGroupKey() }
            .map { (assetId, currencies) ->
                createListItem(
                    userWalletId = value?.userWalletId,
                    assetId = assetId,
                    currencies = currencies,
                    totalFiatBalance = totalFiatBalance,
                )
            }

        // Assets beyond the top ones are collapsed into a single non-expandable "Other" row at the bottom.
        val tokenList = if (otherAssets.count() > 0) {
            assetItems + createOtherItem(otherAssets, totalFiatBalance)
        } else {
            assetItems
        }.toPersistentList()

        val marketChartUM = ForYouPortfolioReviewMarketChartConverter(
            appCurrency = appCurrency,
            topAssets = topAssets,
        ).convert(value?.totalFiatBalance)

        return PortfolioReviewUM.Content(
            tokenList = tokenList,
            marketChartUM = marketChartUM,
            onAddFundsClick = null,
        )
    }

    private fun createListItem(
        userWalletId: UserWalletId?,
        assetId: String,
        currencies: List<CryptoCurrencyStatus>,
        totalFiatBalance: BigDecimal,
    ): ForYouTokenListItemUM {
        // Group the asset's holdings by blockchain (network.id.rawId, derivation-independent) so each
        // network appears once even when the asset is held across several accounts/derivations on it,
        // summing those balances. Order by balance so the expanded breakdown reads top-down.
        val networkGroups = currencies
            .groupBy { it.currency.network.id.rawId }
            .values
            .sortedByDescending { group -> group.sumOf { it.value.fiatAmount.orZero() } }

        val rowConverter = ForYouPortfolioReviewTokenRowConverter(
            userWalletId = userWalletId,
            appCurrency = appCurrency,
            totalFiatBalance = totalFiatBalance,
            onTokenClick = onTokenClick,
        )

        return ForYouTokenListItemUM(
            tokenRowUM = createAssetRow(
                userWalletId = userWalletId,
                assetId = assetId,
                currencies = currencies,
                networkCount = networkGroups.size,
                totalFiatBalance = totalFiatBalance,
            ),
            tokenList = networkGroups.map(rowConverter::convert).toPersistentList(),
            isExpanded = assetId in expandedAssetIds,
            isExpandable = true,
        )
    }

    private fun createAssetRow(
        assetId: String,
        userWalletId: UserWalletId?,
        currencies: List<CryptoCurrencyStatus>,
        networkCount: Int,
        totalFiatBalance: BigDecimal,
    ): TangemTokenRowUM {
        if (currencies.all { it.value is CryptoCurrencyStatus.Loading }) {
            return TangemTokenRowUM.Loading(id = assetId)
        }

        val asset = currencies.first()
        val assetFiatBalance = currencies.sumOf { it.value.fiatAmount.orZero() }

        val rowConverter = ForYouPortfolioReviewTokenRowConverter(
            userWalletId = userWalletId,
            appCurrency = appCurrency,
            totalFiatBalance = totalFiatBalance,
            onTokenClick = onTokenClick,
        )
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

    private fun createOtherItem(
        otherAssets: List<Pair<List<CryptoCurrencyStatus>, BigDecimal>>,
        totalFiatBalance: BigDecimal,
    ): ForYouTokenListItemUM {
        val otherAssetsBalance = otherAssets.sumOf { (_, assetBalance) -> assetBalance }
        return ForYouTokenListItemUM(
            tokenRowUM = TangemTokenRowUM.Content(
                id = OTHER_ROW_ID,
                headIconUM = TangemIconUM.Currency(CurrencyIconState.Empty()),
                titleUM = TangemTokenRowUM.TitleUM.Content(text = resourceReference(R.string.common_other)),
                subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                    text = pluralReference(
                        id = R.plurals.market_chart_assets_android,
                        count = otherAssets.count(),
                        formatArgs = wrappedList(otherAssets.count()),
                    ),
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
        const val TOP_HOLDINGS_COUNT = 4
    }
}