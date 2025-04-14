package com.tangem.data.walletconnect.sign

import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.usecase.WcMethodUseCase
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import com.tangem.domain.walletconnect.usecase.sign.WcSignUseCase
import kotlinx.coroutines.flow.FlowCollector

internal abstract class BaseWcSignUseCase<MiddleAction, SignModel> :
    WcMethodUseCase,
    WcSignUseCase.FinalAction,
    FinalActionCollector<SignModel>,
    MiddleActionCollector<MiddleAction, SignModel> {

    abstract val respondService: WcRespondService

    abstract val context: WcMethodUseCaseContext
    override val network: Network get() = context.network
    override val session: WcSession get() = context.session
    override val rawSdkRequest: WcSdkSessionRequest get() = context.rawSdkRequest

    protected val delegate by lazy {
        WcSignUseCaseDelegate(
            finalActionCollector = this,
            middleActionCollector = this,
        )
    }

    override val onCancel: suspend (currentState: WcSignState<SignModel>) -> Unit = {
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

    val onMiddleAction: OnMiddle<MiddleAction, SignModel> get() = { _, _ -> }
}

internal interface FinalActionCollector<SignModel> {

    val onSign: OnSign<SignModel> get() = {}

    val onCancel: OnCancel<SignModel> get() = {}
}

internal class WcMethodUseCaseContext(
    val session: WcSession,
    val rawSdkRequest: WcSdkSessionRequest,
    val network: Network,
)

internal typealias OnSign<SignModel> =
    suspend FlowCollector<WcSignState<SignModel>>.(state: WcSignState<SignModel>) -> Unit
internal typealias OnCancel<SignModel> =
    suspend (currentState: WcSignState<SignModel>) -> Unit
internal typealias OnMiddle<MiddleAction, SignModel> =
    suspend FlowCollector<SignModel>.(currentState: WcSignState<SignModel>, middleAction: MiddleAction) -> Unit