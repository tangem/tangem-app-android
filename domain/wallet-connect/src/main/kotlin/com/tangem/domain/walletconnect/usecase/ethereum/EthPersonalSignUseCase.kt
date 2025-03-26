package com.tangem.domain.walletconnect.usecase.ethereum

import arrow.core.Either
import com.tangem.domain.walletconnect.model.WcRequest
import com.tangem.domain.walletconnect.respond.WcRespondService
import com.tangem.domain.walletconnect.usecase.WcUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class EthPersonalSignUseCase(
    private val wcRequest: WcRequest<WcEthMethod.SignMessage>,
    private val respondService: WcRespondService,
) : WcUseCase {

    private val onCallTerminalAction = Channel<TerminalAction>()

    fun signFlow(): Flow<State> = flow {
        val model = SignModel(wcRequest.method.raw)
        emit(State.PreSign(model))

        when (val action = onCallTerminalAction.receiveAsFlow().first()) {
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

    @Suppress("FunctionOnlyReturningConstant") // todo(wc) remove later
    private suspend fun signAndPrepareForSend(): String? {
        return null // todo(wc)
    }

    fun sign(toSign: SignModel) {
        onCallTerminalAction.trySend(TerminalAction.Sign(toSign))
    }

    fun cancel() {
        onCallTerminalAction.trySend(TerminalAction.Cancel)
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