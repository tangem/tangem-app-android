package com.tangem.domain.redux.extensions

import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.redux.domainStore
import org.rekotlin.Action

/**
 * Created by Anton Zhilenkov on 24/05/2022.
 */
internal suspend inline fun dispatchOnMain(vararg actions: Action) {
    withMainContext { actions.forEach { domainStore.dispatch(it) } }
}
