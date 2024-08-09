package com.tangem.features.markets.details.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.charts.state.MarketChartDataProducer
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.markets.PriceChangeInterval
import java.math.BigDecimal

internal data class MarketsTokenDetailsUM(
    val tokenName: String,
    val priceText: String,
    val iconUrl: String,
    val dateTimeText: TextReference,
    val priceChangePercentText: String,
    val priceChangeType: PriceChangeType,
    val selectedInterval: PriceChangeInterval,
    val chartState: ChartState,
    val onSelectedIntervalChange: (PriceChangeInterval) -> Unit,
    val infoBottomSheet: TangemBottomSheetConfig,
    val body: Body,
) {

    data class ChartState(
        val status: Status,
        val dataProducer: MarketChartDataProducer,
        val chartLook: MarketChartLook,
        val onLoadRetryClick: () -> Unit,
        val onMarkerPointSelected: (time: BigDecimal?, price: BigDecimal?) -> Unit,
    ) {
        enum class Status {
            LOADING, ERROR, DATA
        }
    }

    data class InformationBlocks(
        val insights: InsightsUM?,
        val securityScore: SecurityScoreUM?,
        val metrics: MetricsUM?,
        val pricePerformance: PricePerformanceUM?,
        val links: LinksUM?,
    )

    @Immutable
    sealed interface Body {

        data class Error(
            val onLoadRetryClick: () -> Unit,
        ) : Body

        data object Loading : Body

        data class Content(
            val description: Description?,
            val infoBlocks: InformationBlocks,
        ) : Body

        data object Nothing : Body
    }

    data class Description(
        val shortDescription: TextReference,
        val fullDescription: TextReference?,
        val onReadMoreClick: () -> Unit,
    )
}