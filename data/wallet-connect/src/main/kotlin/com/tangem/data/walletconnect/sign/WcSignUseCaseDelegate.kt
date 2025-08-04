package com.tangem.data.walletconnect.sign

import arrow.core.left
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.sign.SignStateConverter.toPreSign
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.SignStateConverter.toSigning
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.WcRequestError.Companion.code
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class WcSignUseCaseDelegate<MiddleAction, SignModel>(
    private val analytics: AnalyticsEventHandler,
    private val context: WcMethodUseCaseContext,
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
        analytics.send(
            WcAnalyticEvents.SignatureRequestReceived(
                rawRequest = context.rawSdkRequest,
                network = context.network,
            ),
        )

        state
            .onEach { newState -> channel.send(newState) }
            .launchIn(this)

        fun listenMiddle() = middleActionsChannel.receiveAsFlow()
            .buffer()
            .transform { middleActions -> this.onMiddleAction(state.value.signModel, middleActions) }
            .onEach { updatedModel -> state.update { it.toPreSign(updatedModel) } }
            .launchIn(this)

        var listenMiddleJob: Job = listenMiddle()

        fun signFlow() = flow { onSign(state.updateAndGet { it.toSigning() }) }
            .onEach { newState -> state.update { newState } }
            .catch { exception ->
                val errorResult = state.value
                    .toResult(WcRequestError.UnknownError(exception).left())
                state.update { errorResult }
            }
            .onEach { state ->
                val step = state.domainStep as? WcSignStep.Result ?: return@onEach
                val event = step.result.fold(
                    ifLeft = { error ->
                        WcAnalyticEvents.SignatureRequestFailed(
                            rawRequest = context.rawSdkRequest,
                            network = context.network,
                            errorCode = error.code() ?: error::class.simpleName.orEmpty(),
                        )
                    },
                    ifRight = {
                        WcAnalyticEvents.SignatureRequestHandled(
                            rawRequest = context.rawSdkRequest,
                            network = context.network,
                        )
                    },
                )
                analytics.send(event)
            }

        var signJob: Job? = null

        finalActionsChannel.receiveAsFlow()
            .transformLatest<Action, Unit> { finalAction ->
                when (finalAction) {
                    Action.Cancel -> {
                        onCancel(state.value)
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