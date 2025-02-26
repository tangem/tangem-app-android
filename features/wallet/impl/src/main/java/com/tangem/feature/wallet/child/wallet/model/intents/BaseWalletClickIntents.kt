package com.tangem.feature.wallet.child.wallet.model.intents

import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import kotlinx.coroutines.CoroutineScope
import kotlin.properties.Delegates

/**
 * Base wallet click intents component.
 * Provides router and viewModelScope to child classes.
 *
[REDACTED_AUTHOR]
 */
@Suppress("UnnecessaryAbstractClass")
internal abstract class BaseWalletClickIntents {

    protected val router: InnerWalletRouter get() = _router
    protected val modelScope: CoroutineScope get() = _modelScope

    private var _router: InnerWalletRouter by Delegates.notNull()
    private var _modelScope: CoroutineScope by Delegates.notNull()

    open fun initialize(router: InnerWalletRouter, coroutineScope: CoroutineScope) {
        _router = router
        _modelScope = coroutineScope
    }
}