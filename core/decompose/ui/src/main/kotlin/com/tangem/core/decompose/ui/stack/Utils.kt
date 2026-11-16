/*
 * Copied from Decompose 3.5.0 (extensions-compose-experimental), commit d36078df71276275adc3af0ac993b575b545f932
 * https://github.com/arkivanov/Decompose
 *

 * Licensed under the Apache License, Version 2.0: https://www.apache.org/licenses/LICENSE-2.0
 */

package com.tangem.core.decompose.ui.stack

import com.arkivanov.decompose.router.stack.ChildStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

internal fun <C : Any, T : Any> ChildStack<C, T>.dropLast(): ChildStack<C, T> =
    ChildStack(active = backStack.last(), backStack = backStack.dropLast(1))

internal val ChildStack<*, *>.size: Int get() = items.size

internal suspend inline fun awaitAll(vararg jobs: suspend CoroutineScope.() -> Unit) {
    coroutineScope {
        jobs.map { launch(block = it) }.joinAll()
    }
}