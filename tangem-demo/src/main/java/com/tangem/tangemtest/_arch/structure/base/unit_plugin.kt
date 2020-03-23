package com.tangem.tangemtest._arch.structure.base

/**
[REDACTED_AUTHOR]
 */
typealias PluginCallBackResult = (Any?) -> Unit

interface Plugin : Unit {
    fun invoke(data: Any?, asyncCallback: PluginCallBackResult? = null): Any?
}