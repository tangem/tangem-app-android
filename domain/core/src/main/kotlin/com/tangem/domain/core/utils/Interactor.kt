package com.tangem.domain.core.utils

import kotlinx.coroutines.flow.MutableStateFlow

interface Interactor

abstract class StatefullInteractor<State> : Interactor {
    protected abstract val initialState: State
    private val state = MutableStateFlow(initialState)
}
