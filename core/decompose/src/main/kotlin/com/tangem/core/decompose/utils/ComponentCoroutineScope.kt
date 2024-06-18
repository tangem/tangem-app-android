package com.tangem.core.decompose.utils

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**

 * [CoroutineDispatcherProvider.mainImmediate] dispatcher.
 * */
@Suppress("FunctionName")
internal fun ComponentCoroutineScope(lifecycle: Lifecycle, dispatchers: CoroutineDispatcherProvider): CoroutineScope {
    val scope = CoroutineScope(context = dispatchers.mainImmediate + SupervisorJob())
    lifecycle.doOnDestroy(scope::cancel)

    return scope
}