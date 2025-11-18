package com.tangem.data.networks.converters

import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.utils.converter.TwoWayConverter

private typealias YieldSupplyStatusDataModel = List<NetworkStatusDM.YieldSupplyStatus>
private typealias YieldSupplyStatusDomainModel = Map<CryptoCurrency.ID, YieldSupplyStatus?>

internal class NetworkYieldSupplyStatusConverter(
    rawNetworkId: String,
    derivationPath: Network.DerivationPath,
) : TwoWayConverter<YieldSupplyStatusDataModel, YieldSupplyStatusDomainModel> {

    private val currencyIdConverter = CurrencyIdConverter(rawNetworkId, derivationPath)

    override fun convert(value: YieldSupplyStatusDataModel): YieldSupplyStatusDomainModel {
        return value.associate {
            val id = currencyIdConverter.convert(value = it.id)
            val status = YieldSupplyStatus(
                isActive = it.isActive,
                isInitialized = it.isInitialized,
                isAllowedToSpend = it.isAllowedToSpend,
                effectiveProtocolBalance = it.effectiveProtocolBalance,
            )

            id to status
        }
    }

    override fun convertBack(value: YieldSupplyStatusDomainModel): YieldSupplyStatusDataModel {
        return value.mapNotNull { (currencyId, yieldSupplyStatus) ->
            if (yieldSupplyStatus == null) return@mapNotNull null

            NetworkStatusDM.YieldSupplyStatus(
                id = currencyIdConverter.convertBack(value = currencyId),
                isActive = yieldSupplyStatus.isActive,
                isInitialized = yieldSupplyStatus.isInitialized,
                isAllowedToSpend = yieldSupplyStatus.isAllowedToSpend,
                effectiveProtocolBalance = yieldSupplyStatus.effectiveProtocolBalance,
            )
        }
    }
}