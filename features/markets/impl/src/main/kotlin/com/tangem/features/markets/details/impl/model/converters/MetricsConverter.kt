package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
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

@Stable
internal class MetricsConverter(
    private val appCurrency: Provider<AppCurrency>,
    private val tokenSymbol: String,
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
                                    title = resourceReference(
                                        R.string.markets_token_details_market_capitalization_full,
                                    ),
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
                                    title = resourceReference(R.string.markets_token_details_market_rating_full),
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
                                    title = resourceReference(R.string.markets_token_details_trading_volume_full),
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
                                    title = resourceReference(
                                        R.string.markets_token_details_fully_diluted_valuation_full,
                                    ),
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
                                    title = resourceReference(R.string.markets_token_details_circulating_supply_full),
                                    body = resourceReference(
                                        R.string.markets_token_details_circulating_supply_description,
                                    ),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_max_supply),
                        value = maxSupply.formatMaxSupply(),
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_max_supply_full),
                                    body = resourceReference(R.string.markets_token_details_total_supply_description),
                                ),
                            )
                        },
                    ),
                ),
            )
        }
    }

    private fun BigDecimal?.formatMaxSupply(): String {
        when (this) {
            null -> return StringsSigns.DASH_SIGN
            BigDecimal.ZERO -> return StringsSigns.INFINITY_SIGN
        }

        return this.formatAmount(crypto = true)
    }

    private fun BigDecimal?.formatAmount(crypto: Boolean = false): String {
        if (this == null) return StringsSigns.DASH_SIGN

        return if (crypto) {
            format {
                crypto(
                    symbol = tokenSymbol,
                    decimals = 2,
                ).compact()
            }
        } else {
            val currency = appCurrency()

            format {
                fiat(
                    fiatCurrencyCode = currency.code,
                    fiatCurrencySymbol = currency.symbol,
                ).compact()
            }
        }
    }
}