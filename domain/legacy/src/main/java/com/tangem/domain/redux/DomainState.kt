package com.tangem.domain.redux

import com.tangem.domain.redux.global.DomainGlobalState
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class DomainState(val globalState: DomainGlobalState = DomainGlobalState()) : StateType