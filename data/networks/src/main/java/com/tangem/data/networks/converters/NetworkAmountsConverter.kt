package com.tangem.data.networks.converters

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAmountStatus
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.utils.extensions.mapNotNullValues
import java.math.BigDecimal

private typealias AmountsDataModel = Map<String, BigDecimal>
private typealias AmountsDomainModel = Map<CryptoCurrency.ID, CryptoCurrencyAmountStatus>

/**
 * Converter from [AmountsDataModel] to [AmountsDomainModel] and vice versa
 *
[REDACTED_AUTHOR]
 */
internal object NetworkAmountsConverter : TwoWayConverter<AmountsDataModel, AmountsDomainModel> {

    override fun convert(value: AmountsDataModel): AmountsDomainModel {
        return value
            .mapKeys { CryptoCurrency.ID.fromValue(value = it.key) }
            .mapValues { (_, amount) -> CryptoCurrencyAmountStatus.Loaded(value = amount) }
    }

    override fun convertBack(value: AmountsDomainModel): AmountsDataModel {
        return value
            .mapKeys { (id, _) -> id.value }
            .mapNotNullValues { (_, amount) ->
                when (amount) {
                    is CryptoCurrencyAmountStatus.Loaded -> amount.value
                    is CryptoCurrencyAmountStatus.NotFound -> null
                }
            }
    }
}