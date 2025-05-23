package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.formatHex
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.transaction.usecase.PrepareForSendUseCase
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.method.WcMutableFee
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
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
) : BaseWcSignUseCase<Fee, TransactionData>(),
    WcTransactionUseCase,
    WcMutableFee {

    override val securityStatus = blockAidDelegate.getSecurityStatus(
        network = network,
        method = method,
        rawSdkRequest = rawSdkRequest,
        session = session,
        accountAddress = context.accountAddress,
    )

    override suspend fun SignCollector<TransactionData>.onSign(state: WcSignState<TransactionData>) {
        val hash = prepareForSend(state.signModel, wallet, network)
            .map { it.toHexString().formatHex() }
            .onLeft { error ->
                emit(state.toResult(error.left()))
            }
            .getOrNull()
            ?: return
        val respondResult = respondService.respond(rawSdkRequest, hash)
        emit(state.toResult(respondResult))
    }

    override suspend fun FlowCollector<TransactionData>.onMiddleAction(signModel: TransactionData, fee: Fee) {
        val uncompiled = signModel.requireUncompiled()
        val newState = uncompiled.copy(fee = fee)
        emit(newState)
    }

    override fun updateFee(fee: Fee) {
        middleAction(fee)
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> = flow {
        val transactionData = WcEthTxHelper.createTransactionData(
            dAppFee = dAppFee(),
            network = context.network,
            txParams = method.transaction,
        ) ?: return@flow
        emitAll(delegate.invoke(transactionData))
    }

    override suspend fun dAppFee(): Fee.Ethereum.Legacy? {
        val dAppFee = WcEthTxHelper.getDAppFee(
            network = context.network,
            txParams = method.transaction,
        )
        return dAppFee
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcEthMethod.SignTransaction): WcEthSignTransactionUseCase
    }
}