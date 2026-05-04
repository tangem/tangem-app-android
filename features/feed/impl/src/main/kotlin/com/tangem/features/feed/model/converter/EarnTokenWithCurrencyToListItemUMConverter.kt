package com.tangem.features.feed.model.converter

import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.earn.state.EarnListItemUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class EarnTokenWithCurrencyToListItemUMConverter(
    private val onItemClick: (EarnTokenWithCurrency) -> Unit,
) : Converter<EarnTokenWithCurrency, EarnListItemUM> {

    private val cryptoCurrencyToIconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: EarnTokenWithCurrency): EarnListItemUM {
        return EarnListItemUM(
            network = TextReference.Str(value.networkName),
            symbol = TextReference.Str(value.earnToken.tokenSymbol),
            tokenName = TextReference.Str(value.earnToken.tokenName),
            currencyIconState = cryptoCurrencyToIconStateConverter.convert(value.cryptoCurrency),
            earnValue = when (value.earnToken.rewardType) {
                EarnRewardType.APR -> TextReference.Res(
                    id = R.string.staking_apr_earn_badge,
                    formatArgs = wrappedList(convertPercent(value.earnToken.apy)),
                )
                EarnRewardType.APY -> TextReference.Res(
                    id = R.string.yield_module_earn_badge,
                    formatArgs = wrappedList(convertPercent(value.earnToken.apy)),
                )
            },
            earnTypeTitle = when (value.earnToken.type) {
                EarnType.STAKING -> TextReference.Res(R.string.common_staking)
                EarnType.YIELD -> TextReference.Res(R.string.common_yield_mode)
            },
            earnType = value.earnToken.type,
            onItemClick = { onItemClick(value) },
        )
    }

    private fun convertPercent(value: String): TextReference {
        val percent = BigDecimal(value).format { percent(withPercentSign = false) }
        return TextReference.Str(percent)
    }
}