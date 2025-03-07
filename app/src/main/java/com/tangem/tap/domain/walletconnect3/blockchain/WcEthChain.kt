package com.tangem.tap.domain.walletconnect3.blockchain

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.tap.domain.walletconnect3.*
import com.tangem.tap.domain.walletconnect3.blockchain.WcEthMethod.Name
import com.tangem.tap.domain.walletconnect3.blockchain.WcEthMethod.SignMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class WcEthChain(
    val moshi: Moshi,
    private val respondService: WcRespondService,
) : WcRequestHandler<WcEthMethod>, WcUseCasesFlowProvider {

    override val flow: MutableSharedFlow<WcUseCase> = MutableSharedFlow()

    override fun canHandle(methodName: String): Boolean {
        return Name.entries.find { it.raw == methodName } != null
    }

    override fun deserialize(methodName: String, params: String): WcEthMethod? {
        val name = Name.entries.find { it.raw == methodName } ?: return null
        return when (name) {
            Name.SIGN -> TODO()
            Name.PERSONAL_SIGN -> WcRequestHandler.fromJson<SignMessage>(params, moshi)
            Name.SIGN_TYPE_DATA -> TODO()
            Name.SIGN_TYPE_DATA_V4 -> TODO()
            Name.SIGN_TRANSACTION -> TODO()
            Name.SEND_TRANSACTION -> TODO()
        }
    }

    override fun handle(wcRequest: WcRequest<WcEthMethod>) {
        val useCase = when (wcRequest.method) {
            is SignMessage -> EthPersonalSignUseCase(wcRequest as WcRequest<SignMessage>, respondService)
        }
        flow.tryEmit(useCase)
    }
}

sealed interface WcEthMethod : WcMethod {

    data class SignMessage(
        val raw: List<String>,
    ) : WcEthMethod

    enum class Name(val raw: String) {
        SIGN("eth_sign"),
        PERSONAL_SIGN("personal_sign"),
        SIGN_TYPE_DATA("eth_signTypedData"),
        SIGN_TYPE_DATA_V4("eth_signTypedData_v4"),
        SIGN_TRANSACTION("eth_signTransaction"),
        SEND_TRANSACTION("eth_sendTransaction"),
    }
}

class EthPersonalSignUseCase(
    private val wcRequest: WcRequest<SignMessage>,
    private val respondService: WcRespondService,
) : WcUseCase {

    private val onCallTerminalAction = MutableSharedFlow<TerminalAction>()

    fun signFlow(): Flow<State> = flow {
        val model = SignModel(wcRequest.method.raw)
        emit(State.PreSign(model))

        when (val action = onCallTerminalAction.first()) {
            TerminalAction.Cancel -> {
                val result = respondService.rejectRequest(wcRequest.rawSdkRequest)
                emit(State.Result(result, model))
            }
            is TerminalAction.Sign -> {
                emit(State.Signing(action.toSign))
                val signed = signAndPrepareForSend()
                val result =
                    if (signed != null) {
                        respondService.respond(wcRequest.rawSdkRequest, signed)
                    } else {
                        respondService.rejectRequest(wcRequest.rawSdkRequest)
                    }
                emit(State.Result(result, model))
            }
        }
    }

    private suspend fun signAndPrepareForSend(): String? {
        return null // todo(wc)
    }

    fun sign(toSign: SignModel) {
        onCallTerminalAction.tryEmit(TerminalAction.Sign(toSign))
    }

    fun cancel() {
        onCallTerminalAction.tryEmit(TerminalAction.Cancel)
    }

    sealed interface TerminalAction {
        data class Sign(val toSign: SignModel) : TerminalAction
        data object Cancel : TerminalAction
    }

    sealed interface State {
        val model: SignModel

        data class PreSign(override val model: SignModel) : State
        data class Signing(override val model: SignModel) : State
        data class Result(
            val result: Either<Throwable, Unit>,
            override val model: SignModel,
        ) : State
    }

    data class SignModel(
        val raw: List<String>,
    )
}
