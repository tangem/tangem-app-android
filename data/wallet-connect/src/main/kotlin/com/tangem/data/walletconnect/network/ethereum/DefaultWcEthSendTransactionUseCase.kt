package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.OnSign
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthSendTransactionUseCase
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

internal class DefaultWcEthSendTransactionUseCase @AssistedInject constructor(
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted private val method: WcEthMethod.SendTransaction,
    override val respondService: WcRespondService,
    private val sendTransaction: SendTransactionUseCase,
) : BaseWcSignUseCase<Nothing, TransactionData>(),
    WcEthSendTransactionUseCase {

    override val onSign: OnSign<TransactionData> = collector@{ state ->
        val hash = sendTransaction(state.signModel, wallet, network)
            .onLeft { error ->
                val sendError = IllegalArgumentException(error.toString()) // todo(wc) use domain error
                emit(state.toResult(sendError.left()))
            }
            .getOrNull() ?: return@collector
        val respondHash = if (hash.startsWith(HEX_PREFIX)) hash else HEX_PREFIX + hash
        val respondResult = respondService.respond(rawSdkRequest, respondHash)
        emit(state.toResult(respondResult))
    }

    override fun updateFee(fee: TransactionFee) {
        TODO("Not yet implemented")
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> {
        method
        TODO("Not yet implemented")
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: WcMethodUseCaseContext,
            method: WcEthMethod.SendTransaction,
        ): DefaultWcEthSendTransactionUseCase
    }
}
