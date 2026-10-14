package com.tangem.data.wallets.signing

import com.tangem.common.CompletionResult
import com.tangem.common.core.CompletionCallback
import java.util.concurrent.atomic.AtomicBoolean

internal class OneShotSessionCallback(private val callback: CompletionCallback<Unit>) {

    private val isClosed = AtomicBoolean(false)

    val isOpen: Boolean get() = !isClosed.get()

    fun close(result: CompletionResult<Unit>) {
        if (isClosed.compareAndSet(false, true)) callback(result)
    }

    fun abandon() {
        isClosed.set(true)
    }
}