package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.extensions.isAboveZero

fun Map<AmountType, Amount>.toSendableAmounts(): List<Amount> {
    return this.toList().unzip().second
            .filter { it.type != AmountType.Reserve }
            .filter { it.isAboveZero() }
}