package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.DonutSegmentColor
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.FOR_YOU_TOP_EARN_TOKENS_COUNT
import com.tangem.features.foryou.impl.model.converter.forYouGroupKey
import com.tangem.features.foryou.impl.model.converter.forYouSentimentBadge
import com.tangem.features.foryou.impl.model.converter.toForYouPercent
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

/**
 * Builds the For You portfolio-review list from the accounts selected in the portfolio selector
 * ([ForYouSelectedPortfolio]): groups the selected currency statuses by asset across networks
 * (see [forYouGroupKey]) and maps each group to a [ForYouTokenListItemUM] — an aggregate asset row plus,
 * when the asset spans more than one network, its per-network child rows.
 *
 * The child rows are grouped by network (delegated to [ForYouPortfolioReviewTokenRowConverter]) so a network appears
 * once per asset even if the asset is held on it in several accounts. Because the selection can span
 * several wallets, each [AccountCryptoCurrencyStatus] keeps its owning account (which knows its wallet),
 * so a token click opens the right wallet.
 *
 * Modelled on `TokenListStateConverter` (a list converter delegating to a per-item converter).
 *
 * @property coinIndicators indicator readings keyed by uppercase coin symbol; an asset row (and its
 * child rows) gets a sentiment badge built from its entry for the selected [timeframe], or no badge
 * when the map has no data for the symbol
 */
@Suppress("LongParameterList")
internal class ForYouPortfolioReviewConverter(
    private val appCurrency: AppCurrency,
    private val expandClick: (assetId: String) -> Unit,
    private val onTokenClick: (UserWalletId, CryptoCurrency) -> Unit,
    private val onAddFundsClick: (UserWalletId) -> Unit,
    private val onDiagramTap: () -> Unit,
    private val selectedWalletId: UserWalletId?,
    private val coinIndicators: Map<String, CoinIndicators>,
    private val timeframe: CoinIndicators.Reading.Timeframe,
    private val isBalanceHidden: Boolean = false,
) : Converter<ForYouSelectedPortfolio, PortfolioReviewUM> {

    private val iconConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: ForYouSelectedPortfolio): PortfolioReviewUM {
        val totalFiatBalance = value.totalFiatBalance
        val cryptoCurrencyStatus = value.accountCryptoCurrencyStatuses
        val loadedBalance = totalFiatBalance as? TotalFiatBalance.Loaded
        val totalFiatBalanceAmount = loadedBalance?.amount.orZero()

        if (cryptoCurrencyStatus.all { it.status.value.fiatAmount?.isZero() == true }) {
            return PortfolioReviewUM.Content(
                tokenList = cryptoCurrencyStatus
                    .groupBy { it.status.forYouGroupKey() }
                    .entries
                    .take(FOR_YOU_TOP_EARN_TOKENS_COUNT)
                    .map { (assetId, cryptoCurrencyStatus) ->
                        createListItem(
                            assetId = assetId,
                            cryptoCurrencyStatus = cryptoCurrencyStatus,
                            totalFiatBalance = totalFiatBalanceAmount,
                            index = null,
                        )
                    }.toPersistentList(),
                marketChartUM = MarketChartUM.NoData(
                    title = resourceReference(R.string.market_chart_no_amount),
                    donutText = resourceReference(R.string.market_chart_bubble_no_amount),
                ),
                onAddFundsClick = { selectedWalletId?.let(onAddFundsClick) },
            )
        }

        // Drop only assets we positively know are empty — a resolved, priced zero fiat balance. Currencies
        // whose fiat we couldn't determine (unreachable / no-address / no-quote / still-loading — i.e. any
        // non-content status, which all carry a null fiatAmount) are kept so the converter can still render
        // them with the appropriate treatment instead of hiding a token the user actually holds.
        // Then aggregate the rest into assets (the same token across networks shares its forYouGroupKey)
        // and rank assets by their *summed* fiat balance.
        val rankedAssets = cryptoCurrencyStatus
            .filterNot { it.status.value.fiatAmount?.isZero() == true }
            .groupBy { it.status.forYouGroupKey() }
            .map { (_, networks) -> networks to networks.sumOf { it.status.value.fiatAmount.orZero() } }
            .sortedByDescending { (_, assetBalance) -> assetBalance }

        // The top assets are shown individually (each flattened back to its networks so the converter can
        // regroup them by network); the remaining assets are collapsed into a single "Other" row.
        val topAssets = rankedAssets.take(TOP_HOLDINGS_COUNT)
        val otherAssets = rankedAssets.drop(TOP_HOLDINGS_COUNT)
        val topCurrencies = topAssets.flatMap { (networks, _) -> networks }

        val assetItems = topCurrencies
            .groupBy { it.status.forYouGroupKey() }
            .entries
            .mapIndexed { index, (assetId, group) ->
                createListItem(
                    assetId = assetId,
                    cryptoCurrencyStatus = group,
                    totalFiatBalance = totalFiatBalanceAmount,
                    index = index,
                )
            }

        // Assets beyond the top ones are collapsed into a single non-expandable "Other" row at the bottom.
        val tokenList = if (otherAssets.count() > 0) {
            assetItems + createOtherItem(otherAssets, totalFiatBalanceAmount)
        } else {
            assetItems
        }.toPersistentList()

        val marketChartUM = ForYouPortfolioReviewMarketChartConverter(
            appCurrency = appCurrency,
            topAssets = topAssets.map { (networks, assetBalance) -> networks.map { it.status } to assetBalance },
            onSegmentTap = onDiagramTap,
            isBalanceHidden = isBalanceHidden,
        ).convert(totalFiatBalance)

        return PortfolioReviewUM.Content(
            tokenList = tokenList,
            marketChartUM = marketChartUM,
            onAddFundsClick = null,
        )
    }

    private fun createListItem(
        assetId: String,
        cryptoCurrencyStatus: List<AccountCryptoCurrencyStatus>,
        totalFiatBalance: BigDecimal,
        index: Int?,
    ): ForYouTokenListItemUM {
        // Group the asset's holdings by blockchain (network.id.rawId, derivation-independent) so each
        // network appears once even when the asset is held across several accounts/derivations on it,
        // summing those balances. Order by balance so the expanded breakdown reads top-down.
        val networkGroups = cryptoCurrencyStatus
            .groupBy { accountCryptoCurrencyStatus ->
                accountCryptoCurrencyStatus.status
                    .currency
                    .network
                    .id
                    .rawId
            }
            .values
            .sortedByDescending { group -> group.sumOf { it.status.value.fiatAmount.orZero() } }

        // The badge is per-asset (indicators are keyed by symbol), so it is computed once for the
        // selected timeframe and shared by the asset row and all its per-network child rows.
        val indicatorSymbol = cryptoCurrencyStatus.firstOrNull()?.status?.currency?.symbol
        val titleBadge = if (indicatorSymbol != null) {
            forYouSentimentBadge(
                coinIndicators = coinIndicators[indicatorSymbol.uppercase()],
                timeframe = timeframe,
            )
        } else {
            null
        }

        return ForYouTokenListItemUM(
            tokenRowUM = createAssetRow(
                assetId = assetId,
                currencies = cryptoCurrencyStatus,
                networkCount = networkGroups.size,
                totalFiatBalance = totalFiatBalance,
                badge = titleBadge,
            ),
            tokenList = networkGroups.map { networkGroup ->
                ForYouPortfolioReviewTokenRowConverter(
                    userWalletId = networkGroup.first().account.userWalletId,
                    appCurrency = appCurrency,
                    totalFiatBalance = totalFiatBalance,
                    onTokenClick = onTokenClick,
                    isBalanceHidden = isBalanceHidden,
                    titleBadge = titleBadge,
                ).convert(networkGroup.map { it.status })
            }.toPersistentList(),
            isExpanded = false,
            isExpandable = true,
            segmentColor = index?.let { DonutSegmentColor.entries.getOrNull(index) ?: DonutSegmentColor.Blue },
        )
    }

    private fun createAssetRow(
        assetId: String,
        currencies: List<AccountCryptoCurrencyStatus>,
        networkCount: Int,
        totalFiatBalance: BigDecimal,
        badge: TangemBadgeUM?,
    ): TangemTokenRowUM {
        val statuses = currencies.map { it.status }
        if (statuses.all { it.value is CryptoCurrencyStatus.Loading }) {
            return TangemTokenRowUM.Loading(id = assetId)
        }

        val asset = statuses.first()
        val assetFiatBalance = statuses.sumOf { it.value.fiatAmount.orZero() }

        // The asset row itself only expands/collapses (no token click), so its wallet id is irrelevant —
        // pass the group's representative one to reuse the shared end-content formatting.
        val rowConverter = ForYouPortfolioReviewTokenRowConverter(
            userWalletId = currencies.first().account.userWalletId,
            appCurrency = appCurrency,
            totalFiatBalance = totalFiatBalance,
            onTokenClick = onTokenClick,
            isBalanceHidden = isBalanceHidden,
        )
        val endContent = rowConverter.toEndContent(statuses = statuses, fiatAmount = assetFiatBalance)

        val onlyCryptoCurrency = asset.currency
        val isMain = onlyCryptoCurrency is CryptoCurrency.Coin

        val subtitle = when {
            networkCount > 1 -> pluralReference(
                id = R.plurals.common_networks_count,
                count = networkCount,
                formatArgs = wrappedList(networkCount),
            )
            isMain -> resourceReference(R.string.common_main_network)
            else -> stringReference(onlyCryptoCurrency.network.name)
        }

        return TangemTokenRowUM.Content(
            id = assetId,
            headIconUM = TangemIconUM.Currency(iconConverter.convert(asset)),
            titleUM = TangemTokenRowUM.TitleUM.Content(
                text = stringReference(asset.currency.name),
                badge = badge,
            ),
            subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                text = subtitle,
            ),
            topEndContentUM = endContent.top,
            bottomEndContentUM = endContent.bottom,
            onItemClick = {
                if (networkCount > 1) {
                    expandClick(assetId)
                } else {
                    onTokenClick(
                        currencies.first().account.userWalletId,
                        asset.currency,
                    )
                }
            },
            onItemLongClick = null,
        )
    }

    private fun createOtherItem(
        otherAssets: List<Pair<List<AccountCryptoCurrencyStatus>, BigDecimal>>,
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
                        }.orMaskWithStars(isBalanceHidden),
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
            segmentColor = DonutSegmentColor.Grey,
        )
    }

    private companion object {
        const val OTHER_ROW_ID = "for_you_other_assets"
        const val TOP_HOLDINGS_COUNT = 4
    }
}