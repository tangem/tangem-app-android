package com.tangem.data.transaction

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.FeeRepository

internal class DefaultFeeRepository : FeeRepository {

    override fun isFeeApproximate(networkId: Network.ID, amountType: AmountType): Boolean {
        return networkId.toBlockchain().isFeeApproximate(amountType)
    }
}