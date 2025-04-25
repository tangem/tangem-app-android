package com.tangem.data.walletconnect.network.ethereum

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

internal class BlockAidEthereumVerificationDelegate @Inject constructor(
    private val blockAidVerifier: BlockAidVerifier,
) {

    fun getSecurityStatus(
        network: Network,
        method: WcEthMethod,
        rawSdkRequest: WcSdkSessionRequest,
        session: WcSession,
    ): Flow<Lce<Throwable, CheckTransactionResult>> = flow {
        emit(Lce.Loading(partialContent = null))
        blockAidVerifier.verifyTransaction(
            TransactionData(
                chain = network.name,
                accountAddress = when (method) {
                    is WcEthMethod.MessageSign -> method.account
                    is WcEthMethod.SendTransaction -> method.transaction.from
                    is WcEthMethod.SignTransaction -> method.transaction.from
                },
                method = rawSdkRequest.request.method,
                domainUrl = session.sdkModel.appMetaData.url,
                params = TransactionParams.Evm(rawSdkRequest.request.params),
            ),
        ).fold(
            ifLeft = {
                Timber.e("Failed to verify transaction: ${it.localizedMessage}")
                emit(Lce.Error(it))
            },
            ifRight = {
                emit(Lce.Content(it))
            },
        )
    }
}
