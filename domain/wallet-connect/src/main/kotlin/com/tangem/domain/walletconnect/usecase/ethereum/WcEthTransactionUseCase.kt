package com.tangem.domain.walletconnect.usecase.ethereum

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.walletconnect.usecase.sign.WcSignUseCase

interface WcEthSendTransactionUseCase :
    WcSignUseCase,
    WcSignUseCase.SimpleRun<TransactionData> {

    fun updateFee(fee: TransactionFee)
}

interface WcEthSignTransactionUseCase :
    WcSignUseCase,
    WcSignUseCase.SimpleRun<TransactionData> {

    fun updateFee(fee: TransactionFee)
}

data class WcEthTransaction(
    val isFeeByDap: Boolean,
    val fee: TransactionFee,
)