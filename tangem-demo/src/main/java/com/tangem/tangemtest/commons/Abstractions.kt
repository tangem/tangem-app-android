package com.tangem.tangemtest.commons

/**
[REDACTED_AUTHOR]
 */
interface Store<M> {
    fun save(config: M)
    fun restore(): M
}