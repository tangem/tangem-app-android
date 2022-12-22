package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.transactions.SendTxResult
import java.math.BigDecimal

interface TransactionManager {

    suspend fun sendTransaction(
        networkId: String,
        amountToSend: BigDecimal,
        currencyToSend: Currency,
        feeAmount: BigDecimal,
        destinationAddress: String,
        dataToSign: String,
    ): SendTxResult
}
