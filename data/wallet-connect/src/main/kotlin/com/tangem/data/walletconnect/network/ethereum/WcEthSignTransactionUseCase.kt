package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.Amount
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
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.WcApproval
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
import kotlinx.coroutines.flow.map

internal class WcEthSignTransactionUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    private val prepareForSend: PrepareForSendUseCase,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcEthMethod.SignTransaction,
    blockAidDelegate: BlockAidVerificationDelegate,
) : BaseWcSignUseCase<WcEthTxAction, TransactionData>(),
    WcTransactionUseCase,
    WcApproval,
    WcMutableFee {

    private var approvalAmount: Amount? = null
    private var dAppFee = WcEthTxHelper.getDAppFee(
        network = context.network,
        txParams = method.transaction,
    )

    override val securityStatus = blockAidDelegate.getSecurityStatus(
        network = network,
        method = method,
        rawSdkRequest = rawSdkRequest,
        session = session,
        accountAddress = context.accountAddress,
    ).map { lce ->
        lce.map { result ->
            val amount = WcEthTxHelper.getApprovedAmount(method.transaction.data, result)
                ?: return@map BlockAidTransactionCheck.Result.Plain(result)
            val tokenInfo = amount.tokenInfo
            if (!amount.isUnlimited) {
                approvalAmount = Amount(
                    currencySymbol = tokenInfo.symbol,
                    decimals = tokenInfo.decimals,
                    value = amount.approvedAmount,
                )
            }
            BlockAidTransactionCheck.Result.Approval(
                result = result,
                approval = this,
                tokenInfo = tokenInfo,
                isMutable = true,
            )
        }
    }

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

    override suspend fun FlowCollector<TransactionData>.onMiddleAction(
        signModel: TransactionData,
        action: WcEthTxAction,
    ) {
        val uncompiled = signModel.requireUncompiled()
        val newState = when (action) {
            is WcEthTxAction.UpdateApprovalAmount -> {
                val extras = uncompiled.extras as EthereumTransactionExtras
                val callData = ApprovalERC20TokenCallData(
                    spenderAddress = uncompiled.sourceAddress,
                    amount = action.amount,
                )
                approvalAmount = action.amount
                dAppFee = null
                uncompiled.copy(extras = extras.copy(callData = callData))
            }
            is WcEthTxAction.UpdateFee -> uncompiled.copy(fee = action.fee)
        }

        emit(newState)
    }

    override fun updateFee(fee: Fee) {
        middleAction(WcEthTxAction.UpdateFee(fee))
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> = flow {
        val transactionData = WcEthTxHelper.createTransactionData(
            dAppFee = dAppFee(),
            network = context.network,
            txParams = method.transaction,
        ) ?: return@flow
        emitAll(delegate.invoke(transactionData))
    }

    override fun dAppFee(): Fee.Ethereum.Legacy? {
        return dAppFee
    }

    override fun getAmount(): Amount? {
        return approvalAmount
    }

    override fun updateAmount(amount: Amount?) {
        middleAction(WcEthTxAction.UpdateApprovalAmount(amount))
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcEthMethod.SignTransaction): WcEthSignTransactionUseCase
    }
}