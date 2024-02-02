package com.tangem.data.transaction

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.FeeRepository

internal class DefaultFeeRepository : FeeRepository {
    override fun isFeeApproximate(networkId: Network.ID, amountType: AmountType): Boolean {
        val blockchain = Blockchain.fromId(networkId.value)
        return blockchain.isFeeApproximate(amountType)
    }
}