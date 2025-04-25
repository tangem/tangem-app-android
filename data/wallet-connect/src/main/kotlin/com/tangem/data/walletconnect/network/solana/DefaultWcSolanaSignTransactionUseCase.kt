package com.tangem.data.walletconnect.network.solana

import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.transaction.usecase.PrepareForSendUseCase
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import com.tangem.domain.walletconnect.usecase.solana.WcSolanaSignTransactionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import okio.ByteString.Companion.decodeBase64

internal class DefaultWcSolanaSignTransactionUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    private val prepareForSend: PrepareForSendUseCase,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted private val method: WcSolanaMethod.SignTransaction,
) : BaseWcSignUseCase<Nothing, TransactionData.Compiled>(),
    WcSolanaSignTransactionUseCase {

    override suspend fun SignCollector<TransactionData.Compiled>.onSign(state: WcSignState<TransactionData.Compiled>) {
        val hash = prepareForSend.invoke(transactionData = state.signModel, userWallet = wallet, network = network)
            .onLeft { error ->
                emit(state.toResult(error.left()))
            }
            .getOrNull()
            ?: return
        val respond = "{ signature: \"${hash.encodeBase58()}\" }"
        val respondResult = respondService.respond(rawSdkRequest, respond)
        emit(state.toResult(respondResult))
    }

    override fun invoke(): Flow<WcSignState<TransactionData.Compiled>> {
        val data = method.transaction.decodeBase64()?.toByteArray() ?: ByteArray(0)

        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.Bytes(data),
        )
        return delegate.invoke(transactionData)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: WcMethodUseCaseContext,
            method: WcSolanaMethod.SignTransaction,
        ): DefaultWcSolanaSignTransactionUseCase
    }
}