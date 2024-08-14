package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.InfoPointUM
import com.tangem.features.markets.details.impl.ui.state.InsightsUM
import com.tangem.features.markets.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Stable
internal class InsightsConverter(
    private val appCurrency: Provider<AppCurrency>,
    private val onInfoClick: (InfoBottomSheetContent) -> Unit,
) : Converter<TokenMarketInfo.Insights, InsightsUM> {

    override fun convert(value: TokenMarketInfo.Insights): InsightsUM {
        return with(value) {
            InsightsUM(
                h24Info = createInfoPointList(
                    experiencedBuyerChange = experiencedBuyerChange?.day,
                    holdersChange = holdersChange?.day,
                    liquidityChange = liquidityChange?.day,
                    buyPressureChange = buyPressureChange?.day,
                ),
                weekInfo = createInfoPointList(
                    experiencedBuyerChange = experiencedBuyerChange?.week,
                    holdersChange = holdersChange?.week,
                    liquidityChange = liquidityChange?.week,
                    buyPressureChange = buyPressureChange?.week,
                ),
                monthInfo = createInfoPointList(
                    experiencedBuyerChange = experiencedBuyerChange?.month,
                    holdersChange = holdersChange?.month,
                    liquidityChange = liquidityChange?.month,
                    buyPressureChange = buyPressureChange?.month,
                ),
                onInfoClick = {
                    onInfoClick(
                        InfoBottomSheetContent(
                            title = resourceReference(R.string.markets_token_details_insights),
                            body = stringReference("//TODO"),
                        ),
                    )
                },
            )
        }
    }

    private fun createInfoPointList(
        experiencedBuyerChange: BigDecimal?,
        holdersChange: BigDecimal?,
        liquidityChange: BigDecimal?,
        buyPressureChange: BigDecimal?,
    ): ImmutableList<InfoPointUM> {
        return persistentListOf(
            InfoPointUM(
                title = resourceReference(R.string.markets_token_details_experienced_buyers),
                value = experiencedBuyerChange.convertChange(),
                change = experiencedBuyerChange.changeType(),
                onInfoClick = {
                    onInfoClick(
                        InfoBottomSheetContent(
                            title = resourceReference(R.string.markets_token_details_experienced_buyers),
                            body = resourceReference(R.string.markets_token_details_experienced_buyers_description),
                        ),
                    )
                },
            ),
            InfoPointUM(
                title = resourceReference(R.string.markets_token_details_buy_pressure),
                value = buyPressureChange.convertChange(isFiatValue = true),
                change = buyPressureChange.changeType(),
                onInfoClick = {
                    onInfoClick(
                        InfoBottomSheetContent(
                            title = resourceReference(R.string.markets_token_details_buy_pressure),
                            body = resourceReference(R.string.markets_token_details_buy_pressure_description),
                        ),
                    )
                },
            ),
            InfoPointUM(
                title = resourceReference(R.string.markets_token_details_holders),
                value = holdersChange.convertChange(),
                change = holdersChange.changeType(),
                onInfoClick = {
                    onInfoClick(
                        InfoBottomSheetContent(
                            title = resourceReference(R.string.markets_token_details_holders),
                            body = resourceReference(R.string.markets_token_details_holders_description),
                        ),
                    )
                },
            ),
            InfoPointUM(
                title = resourceReference(R.string.markets_token_details_liquidity),
                value = liquidityChange.convertChange(),
                change = liquidityChange.changeType(),
                onInfoClick = {
                    onInfoClick(
                        InfoBottomSheetContent(
                            title = resourceReference(R.string.markets_token_details_liquidity),
                            body = resourceReference(R.string.markets_token_details_liquidity_description),
                        ),
                    )
                },
            ),
        )
    }

    private fun BigDecimal?.changeType(): InfoPointUM.ChangeType? {
        return when {
            this == null -> null
            this > BigDecimal.ZERO -> InfoPointUM.ChangeType.UP
            this < BigDecimal.ZERO -> InfoPointUM.ChangeType.DOWN
            else -> null
        }
    }

    private fun BigDecimal?.convertChange(isFiatValue: Boolean = false): String {
        if (this == null) return StringsSigns.DASH_SIGN

        val value = if (isFiatValue) {
            val currency = appCurrency()
            BigDecimalFormatter.formatCompactFiatAmount(
                amount = this.abs(),
                fiatCurrencyCode = currency.code,
                fiatCurrencySymbol = currency.symbol,
            )
        } else {
            BigDecimalFormatter.formatCompactAmount(amount = this.abs())
        }

        val spacing = if (isFiatValue) " " else ""

        return when {
            this > BigDecimal.ZERO -> StringsSigns.PLUS + spacing + value
            this < BigDecimal.ZERO -> StringsSigns.MINUS + spacing + value
            this == BigDecimal.ZERO -> value
            else -> StringsSigns.DASH_SIGN
        }
    }
}