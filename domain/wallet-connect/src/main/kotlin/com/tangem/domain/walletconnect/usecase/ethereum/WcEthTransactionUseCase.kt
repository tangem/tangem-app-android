package com.tangem.domain.walletconnect.usecase.ethereum

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.WcMutableFee
import com.tangem.domain.walletconnect.usecase.sign.WcSignUseCase

interface WcEthSendTransactionUseCase :
    WcSignUseCase,
    WcSignUseCase.SimpleRun<WcEthTransaction>,
    WcMutableFee {

    override val session: WcSession
    override val rawSdkRequest: WcSdkSessionRequest
    override val network: Network

    override fun sign()
    override fun cancel()
    override fun updateFee(fee: Fee)
}

interface WcEthSignTransactionUseCase :
    WcSignUseCase,
    WcSignUseCase.SimpleRun<WcEthTransaction>,
    WcMutableFee {

    override val session: WcSession
    override val rawSdkRequest: WcSdkSessionRequest
    override val network: Network

    override fun sign()
    override fun cancel()
    override fun updateFee(fee: Fee)
}

data class WcEthTransaction(
    val dAppFee: Fee.Ethereum.Legacy?,
    val transactionData: TransactionData.Uncompiled,
)