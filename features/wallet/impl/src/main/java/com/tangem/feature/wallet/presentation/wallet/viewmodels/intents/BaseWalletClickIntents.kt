package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

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
    protected val viewModelScope: CoroutineScope get() = _viewModelScope

    private var _router: InnerWalletRouter by Delegates.notNull()
    private var _viewModelScope: CoroutineScope by Delegates.notNull()

    open fun initialize(router: InnerWalletRouter, coroutineScope: CoroutineScope) {
        _router = router
        _viewModelScope = coroutineScope
    }
}