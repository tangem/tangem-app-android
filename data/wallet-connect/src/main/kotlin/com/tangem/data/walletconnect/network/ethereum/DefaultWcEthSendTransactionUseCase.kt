package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.formatHex
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthSendTransactionUseCase
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthTransaction
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class DefaultWcEthSendTransactionUseCase @AssistedInject constructor(
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted private val method: WcEthMethod.SendTransaction,
    override val respondService: WcRespondService,
    private val sendTransaction: SendTransactionUseCase,
    blockAidDelegate: BlockAidVerificationDelegate,
) : BaseWcSignUseCase<Fee, WcEthTransaction>(),
    WcEthSendTransactionUseCase {

    override val securityStatus = blockAidDelegate.getSecurityStatus(
        network = network,
        method = method,
        rawSdkRequest = rawSdkRequest,
        session = session,
        accountAddress = context.accountAddress,
    )

    private val converter = EthTransactionParamsConverter(context)

    override suspend fun SignCollector<WcEthTransaction>.onSign(state: WcSignState<WcEthTransaction>) {
        val hash = sendTransaction(state.signModel.transactionData, wallet, network)
            .onLeft { error ->
                val sendError = IllegalArgumentException(error.toString()) // todo(wc) use domain error
                emit(state.toResult(sendError.left()))
            }
            .getOrNull() ?: return
        val respondResult = respondService.respond(rawSdkRequest, hash.formatHex())
        emit(state.toResult(respondResult))
    }

    override fun updateFee(fee: Fee) {
        middleAction(fee)
    }

    override fun invoke(): Flow<WcSignState<WcEthTransaction>> = flow {
        val ethTransaction = converter.convert(method.transaction) ?: return@flow
        emitAll(delegate.invoke(ethTransaction))
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: WcMethodUseCaseContext,
            method: WcEthMethod.SendTransaction,
        ): DefaultWcEthSendTransactionUseCase
    }
}