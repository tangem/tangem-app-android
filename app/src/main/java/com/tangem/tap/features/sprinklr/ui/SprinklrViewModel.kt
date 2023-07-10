package com.tangem.tap.features.sprinklr.ui

import androidx.lifecycle.ViewModel
import com.google.accompanist.web.WebContent
import com.tangem.core.navigation.NavigationAction
import com.tangem.tap.common.extensions.dispatchOnMain
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
                sprinklrDomains = state.sprinklrDomains,
                onNewUrl = { updateWebContentOrOpenExternalUrl(it) },
            )
        }
    }

    override fun onCleared() {
        store.unsubscribe(this)
    }

    fun setNavigateBackCallback(callback: () -> Unit) {
        stateInternal.update { prevState ->
            prevState.copy(
                onNavigateBack = callback,
            )
        }
    }

    private fun subscribeToStoreChanges() {
        store.subscribe(this) { appState ->
            appState.skip { old, new -> old.sprinklrState == new.sprinklrState }
                .select { it.sprinklrState }
        }
    }

    private fun WebContent.updateWebContentOrOpenExternalUrl(url: String): WebContent {
        return if (isExternalUrl(url)) {
            store.dispatchOnMain(NavigationAction.OpenUrl(url))
            this
        } else {
            when (this) {
                is WebContent.Url -> copy(url = url)
                else -> WebContent.Url(url)
            }
        }
    }

    private fun isExternalUrl(url: String): Boolean {
        return state.value.sprinklrDomains.none { url.contains(it) }
    }
}