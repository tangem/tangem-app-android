package com.tangem.data.walletconnect.network.solana

import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.encodeBase64
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.transaction.usecase.PrepareForSendUseCase
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import com.tangem.domain.walletconnect.usecase.solana.WcSolanaSignAllTransactionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import okio.ByteString.Companion.decodeBase64
import org.json.JSONArray
import org.json.JSONObject

internal class DefaultWcSolanaSignAllTransactionUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    private val prepareForSend: PrepareForSendUseCase,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted private val method: WcSolanaMethod.SignAllTransaction,
) : BaseWcSignUseCase<Nothing, List<TransactionData.Compiled>>(),
    WcSolanaSignAllTransactionUseCase {

    override suspend fun SignCollector<List<TransactionData.Compiled>>.onSign(
        state: WcSignState<List<TransactionData.Compiled>>,
    ) {
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

    override fun invoke(): Flow<WcSignState<List<TransactionData.Compiled>>> {
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
        ): DefaultWcSolanaSignAllTransactionUseCase
    }
}