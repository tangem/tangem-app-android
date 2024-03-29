package com.tangem.domain.redux

import com.tangem.domain.redux.global.DomainGlobalState
import org.rekotlin.StateType

/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
data class DomainState(val globalState: DomainGlobalState = DomainGlobalState()) : StateType
