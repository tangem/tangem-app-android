package com.tangem.domain

import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState
import com.tangem.domain.redux.state.ActionStateLoggerImpl

/**
[REDACTED_AUTHOR]
 */
object DomainLayer {
    internal val actionStateLogger = ActionStateLoggerImpl()

    var onInitComplete: ((DomainModuleError?) -> Unit)? = null

    fun init() {
        initActionStateLogger()

        onInitComplete?.invoke(null)
    }

    private fun initActionStateLogger() {
        val factory = actionStateLogger.actionStateConvertersFactory

        factory.addConverter(AddCustomTokenAction::class.java, AddCustomTokenState.Converter())
    }
}