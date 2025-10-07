package com.tangem.core.ui.decompose

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface ComposableListContentComponent<T> {

    val uiState: StateFlow<T>

    fun LazyListScope.content(uiState: T, modifier: Modifier)

    companion object {
        val EMPTY = EmptyComposableListContentComponent
    }
}

object EmptyComposableListContentComponent : ComposableListContentComponent<Unit> {
    override val uiState: StateFlow<Unit> = MutableStateFlow(Unit)

    override fun LazyListScope.content(uiState: Unit, modifier: Modifier) { /* no-op */
    }
}