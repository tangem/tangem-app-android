package com.tangem.utils.extensions

import java.lang.ref.WeakReference

/**
 * @author Anton Zhilenkov on 13.06.2023.
 */
fun <T> T.toWeakReference(): WeakReference<T> {
    return WeakReference(this)
}
