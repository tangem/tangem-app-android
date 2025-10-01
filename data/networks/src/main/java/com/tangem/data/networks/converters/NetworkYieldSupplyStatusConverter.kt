package com.tangem.data.networks.converters

import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.utils.extensions.mapNotNullValues

private typealias YieldSupplyStatusDataModel = Map<String, NetworkStatusDM.YieldSupplyStatus?>
private typealias YieldSupplyStatusDomainModel = Map<CryptoCurrency.ID, YieldSupplyStatus?>

internal object NetworkYieldSupplyStatusConverter :
    TwoWayConverter<YieldSupplyStatusDataModel, YieldSupplyStatusDomainModel> {

    override fun convert(value: YieldSupplyStatusDataModel): YieldSupplyStatusDomainModel {
        return value
            .mapKeys { CryptoCurrency.ID.fromValue(value = it.key) }
            .mapValues { (_, yieldSupplyStatus) ->
                if (yieldSupplyStatus != null) {
                    YieldSupplyStatus(
                        isActive = yieldSupplyStatus.isActive,
                        isInitialized = yieldSupplyStatus.isInitialized,
                        isAllowedToSpend = yieldSupplyStatus.isAllowedToSpend,
                    )
                } else {
                    null
                }
            }
    }

    override fun convertBack(value: YieldSupplyStatusDomainModel): YieldSupplyStatusDataModel {
        return value
            .mapKeys { (id, _) -> id.value }
            .mapNotNullValues { (_, yieldSupplyStatus) ->
                if (yieldSupplyStatus != null) {
                    NetworkStatusDM.YieldSupplyStatus(
                        isActive = yieldSupplyStatus.isActive,
                        isInitialized = yieldSupplyStatus.isInitialized,
                        isAllowedToSpend = yieldSupplyStatus.isAllowedToSpend,
                    )
                } else {
                    null
                }
            }
    }
}