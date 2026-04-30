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
 * @param blockchainId the blockchain ID associated with the network (e.g. `Blockchain.id`)
 * @param derivationPath the derivation path used for the network
 *
[REDACTED_AUTHOR]
 */
internal class NetworkAmountsConverter(
    blockchainId: String,
    derivationPath: Network.DerivationPath,
) : TwoWayConverter<AmountsDataModel, AmountsDomainModel> {

    private val currencyIdConverter = NetworkCurrencyIdConverter(blockchainId, derivationPath)

    override fun convert(value: AmountsDataModel): AmountsDomainModel {
        return value.associate { currencyAmount ->
            val currencyId = currencyIdConverter.convert(value = currencyAmount.id)
            val amount = NetworkStatus.Amount.Loaded(value = currencyAmount.amount)

            currencyId to amount
        }
    }

    override fun convertBack(value: AmountsDomainModel): AmountsDataModel {
        return value.mapNotNull { (currencyId, networkAmount) ->
            val amount = networkAmount as? NetworkStatus.Amount.Loaded ?: return@mapNotNull null

            CurrencyAmount(
                id = currencyIdConverter.convertBack(value = currencyId),
                amount = amount.value,
            )
        }
    }
}