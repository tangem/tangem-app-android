package com.tangem.tap.features.sprinklr.ui

import androidx.lifecycle.ViewModel
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.sprinklr.redux.SprinklrState
import com.tangem.tap.store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.rekotlin.StoreSubscriber

internal class SprinklrViewModel : ViewModel(), StoreSubscriber<SprinklrState> {
    private val stateInternal = MutableStateFlow(SprinklrScreenState())
    val state = stateInternal.asStateFlow()

    init {
        subscribeToStoreChanges()
    }

    override fun newState(state: SprinklrState) {
        stateInternal.update { prevState ->
            prevState.copy(
                initialUrl = state.url,
                onNavigateBack = this::navigateBack,
            )
        }
    }

    override fun onCleared() {
        store.unsubscribe(this)
    }

    private fun subscribeToStoreChanges() {
        store.subscribe(this) { appState ->
            appState.skip { old, new -> old.sprinklrState == new.sprinklrState }
                .select { it.sprinklrState }
        }
    }

    private fun navigateBack() {
        store.dispatchOnMain(NavigationAction.PopBackTo())
    }
}