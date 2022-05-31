package com.tangem.domain.redux.extensions

import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.redux.domainStore
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
internal suspend inline fun dispatchOnMain(vararg actions: Action) {
    withMainContext { actions.forEach { domainStore.dispatch(it) } }
}