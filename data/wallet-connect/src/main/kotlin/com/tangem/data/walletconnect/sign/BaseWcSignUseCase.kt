package com.tangem.data.walletconnect.sign

import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignUseCase
import kotlinx.coroutines.flow.FlowCollector

internal abstract class BaseWcSignUseCase<MiddleAction, SignModel> :
    WcMethodUseCase,
    WcSignUseCase,
    FinalActionCollector<SignModel>,
    MiddleActionCollector<MiddleAction, SignModel> {

    abstract val respondService: WcRespondService

    abstract val context: WcMethodUseCaseContext
    override val network: Network get() = context.network
    override val session: WcSession get() = context.session
    override val walletAddress: String get() = context.accountAddress
    override val rawSdkRequest: WcSdkSessionRequest get() = context.rawSdkRequest

    protected val delegate by lazy {
        WcSignUseCaseDelegate(
            finalActionCollector = this,
            middleActionCollector = this,
        )
    }

    override suspend fun onCancel(currentState: WcSignState<SignModel>) {
        defaultReject()
    }

    override fun sign() = delegate.sign()
    override fun cancel() = delegate.cancel()
    protected fun middleAction(action: MiddleAction) = delegate.middleAction(action)

    protected fun defaultReject() {
        respondService.rejectRequestNonBlock(rawSdkRequest)
    }
}

internal interface MiddleActionCollector<MiddleAction, SignModel> {

    suspend fun FlowCollector<SignModel>.onMiddleAction(signModel: SignModel, middleAction: MiddleAction) {}
}

internal interface FinalActionCollector<SignModel> {

    suspend fun SignCollector<SignModel>.onSign(state: WcSignState<SignModel>) {}

    suspend fun onCancel(state: WcSignState<SignModel>) {}
}

internal class WcMethodUseCaseContext(
    val session: WcSession,
    val rawSdkRequest: WcSdkSessionRequest,
    val network: Network,
    val accountAddress: String,
)

internal typealias SignCollector<SignModel> = FlowCollector<WcSignState<SignModel>>