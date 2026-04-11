package com.tangem.data.walletconnect.network.ton

import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.transaction.usecase.PrepareAndSignUseCase
import com.tangem.domain.walletconnect.error.parseSendError
import com.tangem.domain.walletconnect.model.WcTonMethod
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import okio.ByteString.Companion.toByteString

internal class WcTonSendMessageUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    private val prepareAndSign: PrepareAndSignUseCase,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcTonMethod.SendMessage,
) : BaseWcSignUseCase<Nothing, TransactionData>(),
    WcTransactionUseCase {

    override val securityStatus: LceFlow<Throwable, BlockAidTransactionCheck.Result> = emptyFlow()

    override suspend fun SignCollector<TransactionData>.onSign(state: WcSignState<TransactionData>) {
        val signedBytes = prepareAndSign.invoke(
            transactionData = state.signModel,
            userWallet = wallet,
            network = network,
        )
            .onLeft { error -> emit(state.toResult(parseSendError(error).left())) }
            .getOrNull() ?: return

        val respond = signedBytes.toByteString().base64()
        val respondResult = respondService.respond(rawSdkRequest, respond)
        emit(state.toResult(respondResult))
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> {
        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.RawString(rawSdkRequest.request.params),
        )
        return delegate.invoke(transactionData)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcTonMethod.SendMessage): WcTonSendMessageUseCase
    }
}