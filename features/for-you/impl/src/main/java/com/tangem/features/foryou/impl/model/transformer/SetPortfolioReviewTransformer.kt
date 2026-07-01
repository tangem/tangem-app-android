package com.tangem.features.foryou.impl.model.transformer

import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletListUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.converter.ForYouTokenListConverter
import com.tangem.features.foryou.impl.model.converter.forYouGroupKey
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Builds the [ForYouUM] state for the For You screen: the wallet tabs plus the portfolio
 * review (asset count, top-holding share, period picker and the grouped token list).
 *
 * The token list is delegated to [ForYouTokenListConverter]; the period picker selection is carried
 * over from the previous state so it is not reset on every balance refresh.
 *
 * Modelled on `SetTokenListTransformer` (a transformer that rebuilds the state while delegating the
 * token-list construction to a dedicated converter).
 */
@Suppress("LongParameterList")
internal class SetPortfolioReviewTransformer(
    private val walletListUM: WalletListUM,
    private val currencies: List<CryptoCurrencyStatus>,
    private val totalFiatBalance: BigDecimal,
    private val appCurrency: AppCurrency,
    private val expandedAssetIds: Set<String>,
    private val expandClick: (assetId: String) -> Unit,
    private val onPeriodClick: (TangemSegmentUM) -> Unit,
) : Transformer<ForYouUM> {

    override fun transform(prevState: ForYouUM): ForYouUM {
        // Drop empty networks, then aggregate the rest into assets (the same token across networks shares
        // its forYouGroupKey) and rank assets by their *summed* fiat balance.
        val rankedAssets = currencies
            .filterNot { it.value.fiatAmount.orZero().isZero() }
            .groupBy { it.forYouGroupKey() }
            .map { (_, networks) -> networks to networks.sumOf { it.value.fiatAmount.orZero() } }
            .sortedByDescending { (_, assetBalance) -> assetBalance }

        // The top assets are shown individually (each flattened back to its networks so the converter can
        // regroup them by network); the remaining assets are collapsed into a single "Other" row.
        val topAssets = rankedAssets.take(TOP_HOLDINGS_COUNT)
        val otherAssets = rankedAssets.drop(TOP_HOLDINGS_COUNT)
        val topCurrencies = topAssets.flatMap { (networks, _) -> networks }
        val topBalance = topAssets.sumOf { (_, assetBalance) -> assetBalance }

        val tokenList = ForYouTokenListConverter(
            appCurrency = appCurrency,
            totalFiatBalance = totalFiatBalance,
            expandedAssetIds = expandedAssetIds,
            expandClick = expandClick,
            otherAssetCount = otherAssets.size,
            otherFiatBalance = otherAssets.sumOf { (_, assetBalance) -> assetBalance },
        ).convert(topCurrencies)

        return prevState.copy(
            walletListUM = walletListUM,
            portfolioReviewUM = PortfolioReviewUM.Content(
                assetCount = stringReference("${rankedAssets.size} assets"), // TODO For You lokalize
                topHoldingPercent = stringReference("Top holding ${topHoldingPercent(topBalance)}"),
                periodPickerUM = when (prevState.portfolioReviewUM) {
                    is PortfolioReviewUM.Content -> prevState.portfolioReviewUM.periodPickerUM
                    is PortfolioReviewUM.Loading -> createPeriodPicker()
                },
                tokenList = tokenList,
                onPeriodClick = onPeriodClick,
            ),
        )
    }

    private fun topHoldingPercent(topBalance: BigDecimal): String {
        return if (!totalFiatBalance.isZero() && !topBalance.isZero()) {
            topBalance.divide(totalFiatBalance, RoundingMode.HALF_UP).format { percent() }
        } else {
            StringsSigns.DASH_SIGN
        }
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