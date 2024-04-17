package com.tangem.core.decompose.model

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

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

    override fun onDestroy() {
        runCatching { modelScope.cancel() }
    }
}