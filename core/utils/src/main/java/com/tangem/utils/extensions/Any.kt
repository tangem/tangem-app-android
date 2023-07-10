package com.tangem.utils.extensions

import java.lang.ref.WeakReference

/**
[REDACTED_AUTHOR]
 */
fun <T> T.toWeakReference(): WeakReference<T> {
    return WeakReference(this)
}