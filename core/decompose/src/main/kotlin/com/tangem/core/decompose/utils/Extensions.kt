package com.tangem.core.decompose.utils

import com.arkivanov.decompose.router.stack.StackNavigation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Retrieves the current stack of items from the [com.arkivanov.decompose.router.stack.ChildStack]
 * @return A list representing the current stack of items
 */
suspend fun <T : Any> StackNavigation<T>.stack(): List<T> {
    return suspendCancellableCoroutine { continuation ->
        this.navigate(
            transformer = { stack -> stack },
            onComplete = { newStack, oldStack -> continuation.resume(newStack) },
        )
    }
}