package com.tangem.domain.core.store

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.right
import kotlinx.coroutines.flow.*

abstract class Store<E, S> {
    protected abstract val stateFlow: MutableSharedFlow<Either<E, S>>

    protected abstract fun getInitialState(): S

    protected suspend fun updateState(update: S.() -> S) {
        val state = stateFlow.first().getOrNull() ?: getInitialState()
        val updatedState = update(state).right()

        stateFlow.emit(updatedState)
    }

    protected suspend fun updateError(update: (E?) -> E) {
        val error = stateFlow.first().leftOrNull()
        val updatedError = update(error).left()

        stateFlow.emit(updatedError)
    }

    protected fun <T> Raise<E>.getValues(selector: S.() -> T): Flow<T> {
        return stateFlow
            .map { stateEither ->
                val state = stateEither.bind()
                state.selector()
            }
            .distinctUntilChanged()
    }

    protected fun <T1, T2> Raise<E>.getValues(selector: S.() -> T1, mapOrRaise: Raise<E>.(T1) -> T2): Flow<T2> {
        return getValues(selector)
            .map { value -> mapOrRaise(value) }
            .distinctUntilChanged()
    }
}
