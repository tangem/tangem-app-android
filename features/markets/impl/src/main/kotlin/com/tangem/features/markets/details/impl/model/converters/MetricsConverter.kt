package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.InfoPointUM
import com.tangem.features.markets.details.impl.ui.state.MetricsUM
import com.tangem.features.markets.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

@Stable
internal class MetricsConverter(
    private val appCurrency: Provider<AppCurrency>,
    private val onInfoClick: (InfoBottomSheetContent) -> Unit,
) : Converter<TokenMarketInfo.Metrics, MetricsUM> {

    @Suppress("LongMethod")
    override fun convert(value: TokenMarketInfo.Metrics): MetricsUM {
        return with(value) {
            MetricsUM(
                metrics = persistentListOf(
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_market_capitalization),
                        value = marketCap.formatAmount(),
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_market_capitalization),
                                    body = resourceReference(
                                        R.string.markets_token_details_market_capitalization_description,
                                    ),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_market_rating),
                        value = marketRating?.toString() ?: StringsSigns.DASH_SIGN,
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_market_rating),
                                    body = resourceReference(R.string.markets_token_details_market_rating_description),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_trading_volume),
                        value = volume24h.formatAmount(),
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_trading_volume),
                                    body = resourceReference(
                                        R.string.markets_token_details_trading_volume_24h_description,
                                    ),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_fully_diluted_valuation),
                        value = fullyDilutedValuation.formatAmount(),
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_fully_diluted_valuation),
                                    body = resourceReference(
                                        R.string.markets_token_details_fully_diluted_valuation_description,
                                    ),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_circulating_supply),
                        value = circulatingSupply.formatAmount(crypto = true),
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_circulating_supply),
                                    body = resourceReference(
                                        R.string.markets_token_details_circulating_supply_description,
                                    ),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_total_supply),
                        value = totalSupply.formatAmount(crypto = true),
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_total_supply),
                                    body = resourceReference(R.string.markets_token_details_total_supply_description),
                                ),
                            )
                        },
                    ),
                ),
            )
        }
    }

    private fun BigDecimal?.formatAmount(crypto: Boolean = false): String {
        return if (crypto) {
            val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                maximumFractionDigits = 0
                isGroupingUsed = true
                roundingMode = RoundingMode.HALF_UP
            }
            formatter.format(this)
        } else {
            val currency = appCurrency()
            BigDecimalFormatter.formatFiatAmount(
                fiatAmount = this,
                fiatCurrencyCode = currency.code,
                fiatCurrencySymbol = currency.symbol,
                decimals = 0,
            )
        }
    }
}