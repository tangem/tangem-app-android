package com.tangem.data.walletconnect.sign

import arrow.core.left
import com.tangem.data.walletconnect.sign.SignStateConverter.toPreSign
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.SignStateConverter.toSigning
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import com.tangem.domain.walletconnect.usecase.sign.WcSignStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class WcSignUseCaseDelegate<MiddleAction, SignModel>(
    private val finalActionCollector: FinalActionCollector<SignModel>,
    private val middleActionCollector: MiddleActionCollector<MiddleAction, SignModel>,
) : FinalActionCollector<SignModel> by finalActionCollector,
    MiddleActionCollector<MiddleAction, SignModel> by middleActionCollector {

    private val middleActionsChannel = Channel<MiddleAction>()
    private val finalActionsChannel = Channel<Action>()

    fun cancel() {
        finalActionsChannel.trySend(Action.Cancel)
    }

    fun sign() {
        finalActionsChannel.trySend(Action.Sign)
    }

    fun middleAction(action: MiddleAction) {
        middleActionsChannel.trySend(action)
    }

    operator fun invoke(initModel: SignModel) = channelFlow {
        val state = MutableStateFlow(WcSignState(initModel, WcSignStep.PreSign))

        state
            .onEach { newState -> channel.send(newState) }
            .launchIn(this)

        fun listenMiddle() = middleActionsChannel.receiveAsFlow()
            .buffer()
            .transform { middleActions -> this.onMiddleAction(state.value, middleActions) }
            .onEach { updatedModel -> state.update { it.toPreSign(updatedModel) } }
            .launchIn(this)

        var listenMiddleJob: Job = listenMiddle()

        fun signFlow() = flow { onSign(state.updateAndGet { it.toSigning() }) }
            .onEach { newState -> state.update { newState } }
            .catch { exception ->
                val errorResult = state.value.toResult(exception.left())
                state.update { errorResult }
            }

        var signJob: Job? = null

        finalActionsChannel.receiveAsFlow()
            .transformLatest<Action, Unit> { finalAction ->
                when (finalAction) {
                    Action.Cancel -> {
                        onCancel.invoke(state.value)
                        channel.close()
                    }
                    Action.Sign -> {
                        val isSigningNow = signJob?.isActive == true
                        if (isSigningNow) return@transformLatest
                        listenMiddleJob.cancel()
                        signJob = launch {
                            signFlow().collect()
                            listenMiddleJob = listenMiddle()
                        }
                    }
                }
            }
            .launchIn(this)

        /**
         * keep flow running to attempt re-signing after an error
         * or do something after a successful sign
         */
        awaitClose()
    }

    sealed interface Action {
        data object Cancel : Action
        data object Sign : Action
    }
}