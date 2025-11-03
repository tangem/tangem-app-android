package com.tangem.data.networks.converters

import com.tangem.datasource.local.network.entity.NetworkStatusDM.CurrencyAmount
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.utils.converter.TwoWayConverter

private typealias AmountsDataModel = List<CurrencyAmount>
private typealias AmountsDomainModel = Map<CryptoCurrency.ID, NetworkStatus.Amount>

/**
 * Converter from [AmountsDataModel] to [AmountsDomainModel] and vice versa
 *
 * @param rawNetworkId the raw network ID associated with the currency
 * @param derivationPath the derivation path used for the network
 *
[REDACTED_AUTHOR]
 */
internal class NetworkAmountsConverter(
    rawNetworkId: String,
    derivationPath: Network.DerivationPath,
) : TwoWayConverter<AmountsDataModel, AmountsDomainModel> {

    private val currencyIdConverter = CurrencyIdConverter(rawNetworkId, derivationPath)

    override fun convert(value: AmountsDataModel): AmountsDomainModel {
        return value.associate {
            val currencyId = currencyIdConverter.convert(value = it.id)
            val amount = NetworkStatus.Amount.Loaded(value = it.amount)

            currencyId to amount
        }
    }

    override fun convertBack(value: AmountsDomainModel): AmountsDataModel {
        return value.mapNotNull {
            val amount = it.value as? NetworkStatus.Amount.Loaded ?: return@mapNotNull null

            CurrencyAmount(
                id = currencyIdConverter.convertBack(value = it.key),
                amount = amount.value,
            )
        }
    }
}