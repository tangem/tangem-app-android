package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.rawCompact
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.InfoPointUM
import com.tangem.features.markets.details.impl.ui.state.InsightsUM
import com.tangem.features.markets.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

@Stable
internal class InsightsConverter(
    private val appCurrency: Provider<AppCurrency>,
    private val onInfoClick: (InfoBottomSheetContent) -> Unit,
    private val onIntervalChanged: (PriceChangeInterval) -> Unit,
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
                            body = resourceReference(
                                R.string.markets_insights_info_description_message,
                                wrappedList(value.sourceNetworks.joinToString { it.name }),
                            ),
                        ),
                    )
                },
                onIntervalChanged = onIntervalChanged,
            )
        }
    }

    private fun createInfoPointList(
        experiencedBuyerChange: BigDecimal?,
        holdersChange: BigDecimal?,
        liquidityChange: BigDecimal?,
        buyPressureChange: BigDecimal?,
    ): ImmutableList<InfoPointUM> {
        return listOfNotNull(
            experiencedBuyerChange?.let {
                InfoPointUM(
                    title = resourceReference(R.string.markets_token_details_experienced_buyers),
                    value = experiencedBuyerChange.convertChange(),
                    change = experiencedBuyerChange.changeType(),
                    onInfoClick = {
                        onInfoClick(
                            InfoBottomSheetContent(
                                title = resourceReference(R.string.markets_token_details_experienced_buyers_full),
                                body = resourceReference(R.string.markets_token_details_experienced_buyers_description),
                            ),
                        )
                    },
                )
            },
            buyPressureChange?.let {
                InfoPointUM(
                    title = resourceReference(R.string.markets_token_details_buy_pressure),
                    value = buyPressureChange.convertChange(isFiatValue = true),
                    change = buyPressureChange.changeType(),
                    onInfoClick = {
                        onInfoClick(
                            InfoBottomSheetContent(
                                title = resourceReference(R.string.markets_token_details_buy_pressure_full),
                                body = resourceReference(R.string.markets_token_details_buy_pressure_description),
                            ),
                        )
                    },
                )
            },
            holdersChange?.let {
                InfoPointUM(
                    title = resourceReference(R.string.markets_token_details_holders),
                    value = holdersChange.convertChange(),
                    change = holdersChange.changeType(),
                    onInfoClick = {
                        onInfoClick(
                            InfoBottomSheetContent(
                                title = resourceReference(R.string.markets_token_details_holders_full),
                                body = resourceReference(R.string.markets_token_details_holders_description),
                            ),
                        )
                    },
                )
            },
            liquidityChange?.let {
                InfoPointUM(
                    title = resourceReference(R.string.markets_token_details_liquidity),
                    value = liquidityChange.convertChange(),
                    change = liquidityChange.changeType(),
                    onInfoClick = {
                        onInfoClick(
                            InfoBottomSheetContent(
                                title = resourceReference(R.string.markets_token_details_liquidity_full),
                                body = resourceReference(R.string.markets_token_details_liquidity_description),
                            ),
                        )
                    },
                )
            },
        ).toImmutableList()
    }

    private fun BigDecimal.changeType(): InfoPointUM.ChangeType? {
        return when {
            this > BigDecimal.ZERO -> InfoPointUM.ChangeType.UP
            this < BigDecimal.ZERO -> InfoPointUM.ChangeType.DOWN
            else -> null
        }
    }

    private fun BigDecimal.convertChange(isFiatValue: Boolean = false): String {
        val value = if (isFiatValue) {
            this.abs().format {
                val currency = appCurrency()
                fiat(
                    fiatCurrencyCode = currency.code,
                    fiatCurrencySymbol = currency.symbol,
                ).compact()
            }
        } else {
            this.abs().format {
                rawCompact()
            }
        }

        return when {
            this > BigDecimal.ZERO -> StringsSigns.PLUS + value
            this < BigDecimal.ZERO -> StringsSigns.MINUS + value
            this == BigDecimal.ZERO -> value
            else -> StringsSigns.DASH_SIGN
        }
    }
}