package com.tangem.domain.core.store

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import arrow.core.right
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

internal class TestStore : Store<TestStore.StoreError, TestStore.State>() {

    override val stateFlow: MutableSharedFlow<Either<StoreError, State>> = MutableStateFlow(getInitialState().right())

    override fun getInitialState(): State = State(data = null)

    suspend fun update(data: Int) = updateState { copy(data = data) }

    suspend fun update(e: StoreError) = updateError { e }

    fun Raise<StoreError>.get(): Flow<Int> = getValues(State::data) { ensureNotNull(it) { StoreError } }.filterNotNull()

    object StoreError

    data class State(
        val data: Int?,
    )
}
