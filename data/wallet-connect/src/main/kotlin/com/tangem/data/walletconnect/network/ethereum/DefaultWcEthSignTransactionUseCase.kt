package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.extensions.toHexString
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.OnSign
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.transaction.usecase.PrepareForSendUseCase
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthSignTransactionUseCase
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

internal class DefaultWcEthSignTransactionUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    private val prepareForSend: PrepareForSendUseCase,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted private val method: WcEthMethod.SignTransaction,
) : BaseWcSignUseCase<Nothing, TransactionData>(),
    WcEthSignTransactionUseCase {

    override val onSign: OnSign<TransactionData> = collector@{ state ->
        val hash = prepareForSend(state.signModel, wallet, network)
            .onLeft { error ->
                emit(state.toResult(error.left()))
            }
            .getOrNull() ?: return@collector
        val hashString = hash.toHexString()
        val respondHash = if (hashString.startsWith(HEX_PREFIX)) hashString else HEX_PREFIX + hashString
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
            method: WcEthMethod.SignTransaction,
        ): DefaultWcEthSignTransactionUseCase
    }
}
