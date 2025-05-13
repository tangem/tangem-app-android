package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.formatHex
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.transaction.usecase.PrepareForSendUseCase
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.method.WcEthTransaction
import com.tangem.domain.walletconnect.usecase.method.WcEthTransactionUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class WcEthSignTransactionUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    private val prepareForSend: PrepareForSendUseCase,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcEthMethod.SignTransaction,
    blockAidDelegate: BlockAidVerificationDelegate,
) : BaseWcSignUseCase<Fee, WcEthTransaction>(),
    WcEthTransactionUseCase {

    override val securityStatus = blockAidDelegate.getSecurityStatus(
        network = network,
        method = method,
        rawSdkRequest = rawSdkRequest,
        session = session,
        accountAddress = context.accountAddress,
    )

    private val converter = EthTransactionParamsConverter(context)

    override suspend fun SignCollector<WcEthTransaction>.onSign(state: WcSignState<WcEthTransaction>) {
        val hash = prepareForSend(state.signModel.transactionData, wallet, network)
            .map { it.toHexString().formatHex() }
            .onLeft { error ->
                emit(state.toResult(error.left()))
            }
            .getOrNull()
            ?: return
        val respondResult = respondService.respond(rawSdkRequest, hash)
        emit(state.toResult(respondResult))
    }

    override suspend fun FlowCollector<WcEthTransaction>.onMiddleAction(signModel: WcEthTransaction, fee: Fee) {
        val newState = signModel
            .copy(transactionData = signModel.transactionData.copy(fee = fee))
        emit(newState)
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
        fun create(context: WcMethodUseCaseContext, method: WcEthMethod.SignTransaction): WcEthSignTransactionUseCase
    }
}