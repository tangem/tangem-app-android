package com.tangem.features.feed.rwa.model.list

import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.format.bigdecimal.price
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetTopFiveMarketTokenUseCase
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.rwa.ui.state.RwaFeaturedAssetUM
import com.tangem.features.feed.rwa.ui.state.RwaFeaturedUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.PaginationStatus
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Reuses [GetTopFiveMarketTokenUseCase] (`firstBatchSize = 5`, `nextBatchSize = 0`) rather than a
 * dedicated endpoint, the same way the legacy feed builds its top-tokens rail.
 */
internal class RwaFeaturedAssetsManager(
    getTopFiveMarketTokenUseCase: GetTopFiveMarketTokenUseCase,
    private val appCurrency: StateFlow<AppCurrency>,
    private val onItemClick: (CryptoCurrency.RawID) -> Unit,
    private val modelScope: CoroutineScope,
) {

    private val actionsFlow = MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()

    private val batchFlow = getTopFiveMarketTokenUseCase(
        batchingContext = TokenListBatchingContext(
            actionsFlow = actionsFlow,
            coroutineScope = modelScope,
        ),
        order = TokenMarketListConfig.Order.ByRating,
    )

    val state: StateFlow<RwaFeaturedUM> = batchFlow.state
        .map { batchState ->
            val tokens = batchState.data.flatMap { it.data }

            when {
                tokens.isNotEmpty() -> RwaFeaturedUM.Content(
                    assets = tokens.map(::toFeaturedAsset).toImmutableList(),
                )
                batchState.status is PaginationStatus.InitialLoadingError -> RwaFeaturedUM.Empty
                batchState.status is PaginationStatus.EndOfPagination -> RwaFeaturedUM.Empty
                else -> RwaFeaturedUM.Loading
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = RwaFeaturedUM.Loading,
        )

    fun load(categoryId: String) {
        modelScope.launch {
            actionsFlow.emit(
                BatchAction.Reload(
                    requestParams = TokenMarketListConfig(
                        fiatPriceCurrency = appCurrency.value.code,
                        searchText = null,
                        priceChangeInterval = TokenMarketListConfig.Interval.H24,
                        order = TokenMarketListConfig.Order.ByRating,
                        categoryId = categoryId,
                    ),
                ),
            )
        }
    }

    private fun toFeaturedAsset(token: TokenMarket): RwaFeaturedAssetUM {
        val currency = appCurrency.value
        val changePercent = token.tokenQuotesShort.h24ChangePercent

        return RwaFeaturedAssetUM(
            id = token.id.value,
            iconUrl = token.imageUrlLarge,
            name = stringReference(token.name),
            ticker = stringReference(token.symbol),
            price = stringReference(
                token.tokenQuotesShort.currentPrice.format {
                    fiat(fiatCurrencyCode = currency.code, fiatCurrencySymbol = currency.symbol).price()
                },
            ),
            priceChange = TangemPriceChange.State(
                value = stringReference(changePercent.format { percent() }),
                direction = changePercent.toDirection(),
            ),
            onClick = { onItemClick(token.id) },
        )
    }

    @Suppress("MagicNumber")
    private fun BigDecimal?.toDirection(): TangemPriceChange.Direction {
        val scaled = this?.setScale(4, RoundingMode.HALF_UP)
        return when {
            scaled == null -> TangemPriceChange.Direction.Neutral
            scaled > BigDecimal.ZERO -> TangemPriceChange.Direction.Up
            scaled < BigDecimal.ZERO -> TangemPriceChange.Direction.Down
            else -> TangemPriceChange.Direction.Neutral
        }
    }
}