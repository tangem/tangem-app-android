package com.tangem.core.decompose.model

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.annotation.OverridingMethodsMustInvokeSuper

/**
 * Abstract class for a component's model.
 *
 * It provides access to the coroutine dispatchers and a coroutine scope which will survive re-creation of component
 * and will be destroyed when the component is destroyed.
 *
 * Also, it can inject and use some component features like [Router] and [UiMessageSender].
 */
abstract class Model : InstanceKeeper.Instance {

    /**
     * Provides access to the coroutine dispatchers.
     */
    protected abstract val dispatchers: CoroutineDispatcherProvider

    /**
     * The coroutine scope for the model. That will be cancelled when the model is destroyed.
     */
    protected val modelScope by lazy {
        CoroutineScope(context = dispatchers.mainImmediate + SupervisorJob())
    }

    @OverridingMethodsMustInvokeSuper
    override fun onDestroy() {
        runCatching { modelScope.cancel() }
    }

    /**
     * Launches a coroutine in the model's scope and updates the [progressFlow] with the progress state.
     *
     * @param progressFlow The [SharedFlow] to emit the progress state.
     * @param dispatcher The [CoroutineDispatcher] to launch the coroutine. Default is [Dispatchers.Main.immediate].
     * @param block The block of code to execute.
     *
     * @return The [Job] of the launched coroutine.
     * */
    protected inline fun withProgress(
        progressFlow: MutableSharedFlow<Boolean>,
        dispatcher: CoroutineDispatcher = dispatchers.mainImmediate,
        crossinline block: suspend () -> Unit,
    ): Job = resource(
        acquire = { progressFlow.emit(true) },
        release = { progressFlow.emit(false) },
        dispatcher = dispatcher,
        block = block,
    )

    /**
     * Launches [block] in the model's scope and acquires a resource before executing the block and releases it after.
     *
     * @param acquire The block of code to acquire the resource.
     * @param release The block of code to release the resource.
     * @param dispatcher The [CoroutineDispatcher] to launch the coroutine. Default is [Dispatchers.Main.immediate].
     * @param block The block of code to execute.
     *
     * @return The [Job] of the launched coroutine.
     * */
    protected inline fun resource(
        crossinline acquire: suspend () -> Unit,
        crossinline release: suspend () -> Unit,
        dispatcher: CoroutineDispatcher = dispatchers.mainImmediate,
        crossinline block: suspend () -> Unit,
    ): Job = modelScope.launch(dispatcher) {
        acquire()

        try {
            block()
        } finally {
            withContext(NonCancellable) {
                release()
            }
        }
    }

    /**
     * Converts a cold [Flow] to a hot [SharedFlow] that will be shared in the model's scope.
     *
     * @param started The [SharingStarted] strategy to start sharing the flow. Default is [SharingStarted.WhileSubscribed].
     * @param replay The number of values to replay. Default is `1`.
     *
     * @return The [SharedFlow] that will be shared in the model's scope.
     * @see [Flow.shareIn]
     * */
    protected fun <T> Flow<T>.share(
        started: SharingStarted = SharingStarted.WhileSubscribed(),
        replay: Int = 1,
    ): SharedFlow<T> = shareIn(
        scope = modelScope,
        started = started,
        replay = replay,
    )
}