package com.tangem.devkit._arch.structure.abstraction

/**
[REDACTED_AUTHOR]
 */
typealias PluginCallBackResult = (Any?) -> Item

interface Plugin : Item {
    fun invoke(data: Any?, asyncCallback: PluginCallBackResult? = null): Any?
}