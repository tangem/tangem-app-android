package com.tangem.domain.transaction.models

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency

data class TransactionFeeExtended(
    val transactionFee: TransactionFee,
    val feeTokenId: CryptoCurrency.ID,
)