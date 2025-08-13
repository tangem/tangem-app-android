package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.formatHex
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.walletconnect.error.parseSendError
import com.tangem.domain.walletconnect.model.WcApprovedAmount
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.method.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import com.tangem.blockchain.common.Amount as BlockchainAmount

@Suppress("LongParameterList")
internal class WcEthSendTransactionUseCase @AssistedInject constructor(
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcEthMethod.SendTransaction,
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    private val sendTransaction: SendTransactionUseCase,
    private val ethTxHelper: WcEthTxHelper,
    blockAidDelegate: BlockAidVerificationDelegate,
) : BaseWcSignUseCase<WcEthTxAction, TransactionData>(),
    WcTransactionUseCase,
    WcApproval,
    WcMutableFee {

    private var approvalAmount: WcApprovedAmount? = null
    // in case of change TransactionExtras
    // change approvalAmount for example
    private var isIgnoreDAppFee: Boolean = false
    private var dAppFee: Fee? = null

    override val securityStatus: LceFlow<Throwable, BlockAidTransactionCheck.Result> =
        blockAidDelegate.getSecurityStatus(
            network = network,
            method = method,
            rawSdkRequest = rawSdkRequest,
            session = session,
            accountAddress = context.accountAddress,
        ).map { lce ->
            lce.map { result ->
                val amount = ethTxHelper.getApprovedAmount(method.transaction.data, result)
                    ?: return@map BlockAidTransactionCheck.Result.Plain(result)
                val tokenInfo = amount.tokenInfo
                this@WcEthSendTransactionUseCase.approvalAmount = WcApprovedAmount(
                    amount = if (!amount.isUnlimited) {
                        Amount(
                            currencySymbol = tokenInfo.symbol,
                            decimals = tokenInfo.decimals,
                            value = amount.approvedAmount,
                        )
                    } else {
                        null
                    },
                    logoUrl = tokenInfo.logoUrl,
                    chainId = tokenInfo.chainId,
                )
                BlockAidTransactionCheck.Result.Approval(
                    result = result,
                    approval = this,
                    tokenInfo = tokenInfo,
                    isMutable = true,
                )
            }
        }

    override suspend fun SignCollector<TransactionData>.onSign(state: WcSignState<TransactionData>) {
        val hash = sendTransaction(state.signModel, wallet, network)
            .onLeft { error ->
                emit(state.toResult(parseSendError(error).left()))
            }
            .getOrNull() ?: return
        val respondResult = respondService.respond(rawSdkRequest, hash.formatHex())
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
                    amount = action.amount?.amount?.let {
                        BlockchainAmount(currencySymbol = it.currencySymbol, decimals = it.decimals, value = it.value)
                    },
                )
                approvalAmount = action.amount
                isIgnoreDAppFee = true
                uncompiled.copy(extras = extras.copy(callData = callData))
            }
            is WcEthTxAction.UpdateFee -> uncompiled.copy(fee = action.fee)
        }

        emit(newState)
    }

    override suspend fun dAppFee(): Fee? {
        if (isIgnoreDAppFee) return null
        if (dAppFee != null) return dAppFee
        return ethTxHelper.getDAppFee(method.transaction, wallet, network)
            .also { dAppFee = it }
    }

    override fun updateFee(fee: Fee) {
        middleAction(WcEthTxAction.UpdateFee(fee))
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> = flow {
        val transactionData = ethTxHelper.createTransactionData(
            dAppFee = dAppFee(),
            network = context.network,
            txParams = method.transaction,
        ) ?: return@flow
        emitAll(delegate.invoke(transactionData))
    }

    override fun getAmount(): WcApprovedAmount? {
        return approvalAmount
    }

    override fun updateAmount(amount: WcApprovedAmount?) {
        middleAction(WcEthTxAction.UpdateApprovalAmount(amount))
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcEthMethod.SendTransaction): WcEthSendTransactionUseCase
    }
}