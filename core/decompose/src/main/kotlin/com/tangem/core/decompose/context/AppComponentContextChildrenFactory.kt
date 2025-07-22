package com.tangem.core.decompose.context

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.tangem.core.decompose.di.HiltComponentBuilderOwner
import com.tangem.core.decompose.navigation.NavigationOwner
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.DefaultUiMessageSender
import com.tangem.core.decompose.ui.UiMessageHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.decompose.ui.UiMessageSenderOwner
import com.tangem.core.decompose.utils.ActivityHolder
import com.tangem.core.decompose.utils.ComponentCoroutineScope
import com.tangem.core.decompose.utils.DispatchersOwner
import kotlinx.coroutines.CoroutineScope

/**
 * Creates a new child [AppComponentContext] with the provided [key] and optional [lifecycle].
 *
 * @param key The key to use.
 * @param lifecycle The [Lifecycle] to use. If not provided, the parent's lifecycle will be used.
 * @param router The [Router] to use in the child. If not provided, the parent's router will be used.
 * @param messageHandler The [UiMessageHandler] to use in the child. If not provided, the parent's message sender will
 * be used.

 *
 * @see childByContext
 * */
fun AppComponentContext.child(
    key: String,
    lifecycle: Lifecycle? = null,
    router: Router? = null,
    messageHandler: UiMessageHandler? = null,
): AppComponentContext = childByContext(
    componentContext = childContext(key, lifecycle),
    router = router,
    messageHandler = messageHandler,
)

/**
 * Creates a new child [AppComponentContext] with the provided [componentContext].
 *
 * @param componentContext The [ComponentContext] to use.
 * @param router The [Router] to use in the child. If not provided, the parent's router will be used.
 * @param messageHandler The [UiMessageHandler] to use in the child. If not provided, the parent's message sender will
 * be used.

 *
 * @see child
 * */
fun AppComponentContext.childByContext(
    componentContext: ComponentContext,
    router: Router? = null,
    messageHandler: UiMessageHandler? = null,
): AppComponentContext = object :
    AppComponentContext,
    ComponentContext by componentContext,
    NavigationOwner by this@childByContext,
    UiMessageSenderOwner by this@childByContext,
    DispatchersOwner by this@childByContext,
    HiltComponentBuilderOwner by this@childByContext,
    ActivityHolder by this@childByContext {

    override val tags: HashMap<String, Any> = HashMap()

    override val componentScope: CoroutineScope = ComponentCoroutineScope(lifecycle, dispatchers)

    override val messageSender: UiMessageSender = messageHandler
        ?.let(::DefaultUiMessageSender)
        ?: this@childByContext.messageSender

    override val router: Router
        get() = router ?: this@childByContext.router
}