package com.tangem.data.networks.converters

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.utils.extensions.mapNotNullValues
import java.math.BigDecimal

private typealias AmountsDataModel = Map<String, BigDecimal>
private typealias AmountsDomainModel = Map<CryptoCurrency.ID, NetworkStatus.Amount>

/**
 * Converter from [AmountsDataModel] to [AmountsDomainModel] and vice versa
 *
[REDACTED_AUTHOR]
 */
internal object NetworkAmountsConverter : TwoWayConverter<AmountsDataModel, AmountsDomainModel> {

    override fun convert(value: AmountsDataModel): AmountsDomainModel {
        return value
            .mapKeys { CryptoCurrency.ID.fromValue(value = it.key) }
            .mapValues { (_, amount) -> NetworkStatus.Amount.Loaded(value = amount) }
    }

    override fun convertBack(value: AmountsDomainModel): AmountsDataModel {
        return value
            .mapKeys { (id, _) -> id.value }
            .mapNotNullValues { (_, amount) ->
                when (amount) {
                    is NetworkStatus.Amount.Loaded -> amount.value
                    is NetworkStatus.Amount.NotFound -> null
                }
            }
    }
}