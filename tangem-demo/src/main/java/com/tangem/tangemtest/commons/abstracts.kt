package com.tangem.tangemtest.commons

/**
[REDACTED_AUTHOR]
 */
interface LayoutHolder {
    fun getLayoutId(): Int
}

interface Bindable<T> {
    fun bind(data: T)
}