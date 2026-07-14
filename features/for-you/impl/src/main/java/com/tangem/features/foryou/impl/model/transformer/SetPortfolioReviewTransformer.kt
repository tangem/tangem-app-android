package com.tangem.features.foryou.impl.model.transformer

import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.ForYouNotification
import com.tangem.features.foryou.impl.model.converter.ForYouMarketChartConverter
import com.tangem.features.foryou.impl.model.converter.ForYouTokenListConverter
import com.tangem.features.foryou.impl.model.converter.forYouGroupKey
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

/**
 * Builds the [ForYouUM] state for the For You screen: the outdated-data notifications plus the portfolio
 * review (market chart, period picker and the grouped token list).
 *
 * The token list is delegated to [ForYouTokenListConverter] and the market chart to
 * [ForYouMarketChartConverter]; the period picker selection is carried over from the previous state so
 * it is not reset on every balance refresh.
 *
 * Modelled on `SetTokenListTransformer` (a transformer that rebuilds the state while delegating the
 * token-list construction to a dedicated converter).
 */
@Suppress("LongParameterList")
internal class SetPortfolioReviewTransformer(
    private val accountStatusList: AccountStatusList?,
    private val appCurrency: AppCurrency,
    private val expandedAssetIds: Set<String>,
    private val expandClick: (assetId: String) -> Unit,
    private val onPeriodClick: (TangemSegmentUM) -> Unit,
    private val onTokenClick: (CryptoCurrency) -> Unit,
) : Transformer<ForYouUM> {

    override fun transform(prevState: ForYouUM): ForYouUM {
        val currencies = accountStatusList?.flattenCurrencies().orEmpty()
        val loadedBalance = accountStatusList?.totalFiatBalance as? TotalFiatBalance.Loaded
        val totalFiatBalance = loadedBalance?.amount.orZero()

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

        val tokenList = ForYouTokenListConverter(
            appCurrency = appCurrency,
            totalFiatBalance = totalFiatBalance,
            expandedAssetIds = expandedAssetIds,
            expandClick = expandClick,
            otherAssets = otherAssets,
            onTokenClick = onTokenClick,
        ).convert(topCurrencies)

        val marketChartUM = ForYouMarketChartConverter(
            appCurrency = appCurrency,
            topAssets = topAssets,
        ).convert(accountStatusList?.totalFiatBalance)

        return prevState.copy(
            notifications = if (loadedBalance?.source == StatusSource.ONLY_CACHE) {
                persistentListOf(ForYouNotification.UsedOutdatedData)
            } else {
                persistentListOf()
            },
            portfolioReviewUM = PortfolioReviewUM.Content(
                periodPickerUM = when (prevState.portfolioReviewUM) {
                    is PortfolioReviewUM.Content -> prevState.portfolioReviewUM.periodPickerUM
                    is PortfolioReviewUM.Loading -> createPeriodPicker()
                },
                tokenList = tokenList,
                marketChartUM = marketChartUM,
                onPeriodClick = onPeriodClick,
            ),
        )
    }

    private fun createPeriodPicker(): TangemSegmentedPickerUM {
        // TODO For you replace with data from backend
        val day = TangemSegmentUM(id = "0", title = stringReference("Day"))
        return TangemSegmentedPickerUM(
            items = persistentListOf(
                day,
                TangemSegmentUM(id = "1", title = stringReference("Week")),
                TangemSegmentUM(id = "2", title = stringReference("Month")),
            ),
            initialSelectedItem = day,
            isFixed = true,
            isAltSurface = true,
        )
    }

    private companion object {
        const val TOP_HOLDINGS_COUNT = 4
    }
}