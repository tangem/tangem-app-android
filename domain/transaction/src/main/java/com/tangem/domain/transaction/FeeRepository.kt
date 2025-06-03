package com.tangem.domain.transaction

import com.tangem.blockchain.common.AmountType
import com.tangem.domain.models.network.Network

interface FeeRepository {

    /** Returns if fee is approximate for current [networkId] */
    fun isFeeApproximate(networkId: Network.ID, amountType: AmountType): Boolean
}