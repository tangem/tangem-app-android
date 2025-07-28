package com.tangem.data.walletconnect.network.solana

import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.transaction.usecase.PrepareForSendUseCase
import com.tangem.domain.walletconnect.error.parseSendError
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.ByteString.Companion.decodeBase64

@Suppress("LongParameterList")
internal class WcSolanaSignTransactionUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    private val prepareForSend: PrepareForSendUseCase,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcSolanaMethod.SignTransaction,
    blockAidDelegate: BlockAidVerificationDelegate,
    addressConverter: SolanaBlockAidAddressConverter,
) : BaseWcSignUseCase<Nothing, TransactionData>(),
    WcTransactionUseCase {

    override val securityStatus = blockAidDelegate.getSecurityStatus(
        network = network,
        method = method,
        rawSdkRequest = rawSdkRequest,
        session = session,
        accountAddress = addressConverter.convert(context.accountAddress),
    ).map { lce -> lce.map { result -> BlockAidTransactionCheck.Result.Plain(result) } }

    override suspend fun SignCollector<TransactionData>.onSign(state: WcSignState<TransactionData>) {
        val hash = prepareForSend.invoke(transactionData = state.signModel, userWallet = wallet, network = network)
            .onLeft { error ->
                emit(state.toResult(parseSendError(error).left()))
            }
            .getOrNull()
            ?: return
        val respond = "{ signature: \"${hash.encodeBase58()}\" }"
        val respondResult = respondService.respond(rawSdkRequest, respond)
        emit(state.toResult(respondResult))
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> {
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
        ): WcSolanaSignTransactionUseCase
    }
}