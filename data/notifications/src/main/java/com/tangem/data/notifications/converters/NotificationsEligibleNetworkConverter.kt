package com.tangem.data.notifications.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.datasource.api.tangemTech.models.CryptoNetworkResponse
import com.tangem.domain.notifications.models.NotificationsEligibleNetwork

internal object NotificationsEligibleNetworkConverter {
    fun convert(value: CryptoNetworkResponse): NotificationsEligibleNetwork? {
        val blockchain: Blockchain = Blockchain.fromNetworkId(value.networkId) ?: return null
        return NotificationsEligibleNetwork(
            id = value.networkId,
            name = blockchain.fullName,
            symbol = blockchain.currency,
            icon = getActiveIconRes(blockchain.id),
        )
    }
}