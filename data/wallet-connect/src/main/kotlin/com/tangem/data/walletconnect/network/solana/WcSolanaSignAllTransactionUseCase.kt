package com.tangem.data.walletconnect.network.solana

import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.encodeBase64
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.transaction.usecase.PrepareForSendUseCase
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.WcListTransactionUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.ByteString.Companion.decodeBase64
import org.json.JSONArray
import org.json.JSONObject

internal class WcSolanaSignAllTransactionUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    private val prepareForSend: PrepareForSendUseCase,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcSolanaMethod.SignAllTransaction,
    blockAidDelegate: BlockAidVerificationDelegate,
) : BaseWcSignUseCase<Nothing, List<TransactionData>>(),
    WcListTransactionUseCase {

    override val securityStatus = blockAidDelegate.getSecurityStatus(
        network = network,
        method = method,
        rawSdkRequest = rawSdkRequest,
        session = session,
        accountAddress = context.accountAddress,
    ).map { lce -> lce.map { result -> BlockAidTransactionCheck.Result.Plain(result) } }

    override suspend fun SignCollector<List<TransactionData>>.onSign(state: WcSignState<List<TransactionData>>) {
        val hash = prepareForSend.invoke(transactionData = state.signModel, userWallet = wallet, network = network)
            .onLeft { error ->
                emit(state.toResult(error.left()))
            }
            .getOrNull()
            ?: return
        val respond = getSolanaResultTxHashesString(hash)
        val respondResult = respondService.respond(rawSdkRequest, respond)
        emit(state.toResult(respondResult))
    }

    override fun invoke(): Flow<WcSignState<List<TransactionData>>> {
        val transactionData = method.transaction
            .map { it.decodeBase64()?.toByteArray() ?: ByteArray(0) }
            .map { TransactionData.Compiled(value = TransactionData.Compiled.Data.Bytes(it)) }

        return delegate.invoke(transactionData)
    }

    /**
     * Build json object
     * {
     *   "transactions": [
     *     "signed_tx_hash"
     *   ]
     * }
     */
    private fun getSolanaResultTxHashesString(signedHashes: List<ByteArray>): String {
        val result = JSONObject()
        val transactions = JSONArray()
        signedHashes.forEach {
            transactions.put(it.encodeBase64())
        }
        result.put("transactions", transactions)
        return result.toString()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: WcMethodUseCaseContext,
            method: WcSolanaMethod.SignAllTransaction,
        ): WcSolanaSignAllTransactionUseCase
    }
}