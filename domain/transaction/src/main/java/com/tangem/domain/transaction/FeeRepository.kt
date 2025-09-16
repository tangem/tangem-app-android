package com.tangem.domain.transaction

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet

interface FeeRepository {

    /** Returns if fee is approximate for current [networkId] */
    fun isFeeApproximate(networkId: Network.ID, amountType: AmountType): Boolean

    /** Returns fee calculated for the transaction [transactionData] */
    suspend fun calculateFee(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        transactionData: TransactionData,
    ): TransactionFee
}