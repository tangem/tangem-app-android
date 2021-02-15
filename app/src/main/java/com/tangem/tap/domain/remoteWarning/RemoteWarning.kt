package com.tangem.tap.domain.remoteWarning

import com.tangem.blockchain.common.Blockchain

/**
[REDACTED_AUTHOR]
 */
data class RemoteWarning(
        val title: String,
        val message: String,
        val type: Warning.Type,
        val priority: Warning.Priority,
        val location: List<Warning.Location>,
        val blockchains: List<Blockchain>,
) {
}



sealed class Warning {
    enum class Priority(val priority: String) {
        Info("info"),
        Warning("warning"),
        Critical("critical")
    }
    enum class Type(val type: String) {
        Permanent("permanent"),     // нельзя скрыть
        Temporary("temporary")      // можно скрыть (кнопка ОК)
    }

    enum class Location(val location: String) {
        MainScreen("main"),
        SendScreen("send")
    }
}

