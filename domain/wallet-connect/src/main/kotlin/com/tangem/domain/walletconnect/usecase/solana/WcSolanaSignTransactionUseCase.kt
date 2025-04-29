package com.tangem.domain.walletconnect.usecase.solana

import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.blockaid.WcBlockAidEligibleTransactionUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignUseCase
import kotlinx.coroutines.flow.Flow

interface WcSolanaSignTransactionUseCase :
    WcSignUseCase,
    WcSignUseCase.SimpleRun<TransactionData.Compiled>,
    WcBlockAidEligibleTransactionUseCase {

    override val network: Network
    override val rawSdkRequest: WcSdkSessionRequest
    override val session: WcSession

    override fun sign()
    override fun cancel()
    override fun invoke(): Flow<WcSignState<TransactionData.Compiled>>
}

interface WcSolanaSignAllTransactionUseCase :
    WcSignUseCase,
    WcSignUseCase.SimpleRun<List<TransactionData.Compiled>>,
    WcBlockAidEligibleTransactionUseCase {

    override val network: Network
    override val rawSdkRequest: WcSdkSessionRequest
    override val session: WcSession

    override fun sign()
    override fun cancel()
    override fun invoke(): Flow<WcSignState<List<TransactionData.Compiled>>>
}