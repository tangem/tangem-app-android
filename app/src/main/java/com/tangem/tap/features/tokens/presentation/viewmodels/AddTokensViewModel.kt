package com.tangem.tap.features.tokens.presentation.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.tokens.presentation.router.AddTokensRouter
import com.tangem.tap.features.tokens.presentation.states.AddTokensStateHolder
import com.tangem.tap.features.tokens.presentation.states.AddTokensToolbarState
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.features.tokens.redux.TokensState
import com.tangem.tap.store
import com.tangem.wallet.R
import dagger.hilt.android.lifecycle.HiltViewModel
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

/**
 * ViewModel for add tokens screen
 *
 * @property router feature router
 *
[REDACTED_AUTHOR]
 */
@HiltViewModel
internal class AddTokensViewModel @Inject constructor(
    private val router: AddTokensRouter,
) : ViewModel(), DefaultLifecycleObserver, StoreSubscriber<TokensState> {

    var uiState by mutableStateOf(getInitialUiState())
        private set

    init {
        loadContent()
    }

    override fun onStop(owner: LifecycleOwner) {
        store.unsubscribe(subscriber = this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        store.dispatch(TokensAction.ResetState)
    }

    override fun newState(state: TokensState) {
        uiState = uiState.copySealed(
            toolbarState = getToolbarState(hasEditAccess = state.allowToAdd),
        )
    }

    private fun loadContent() {
        store.subscribe(subscriber = this) { state ->
            state
                .skipRepeats { old, new -> old.tokensState == new.tokensState }
                .select(AppState::tokensState)
        }
    }

    private fun getInitialUiState(): AddTokensStateHolder {
        return AddTokensStateHolder.Loading(toolbarState = getToolbarState(store.state.tokensState.allowToAdd))
    }

    private fun getToolbarState(hasEditAccess: Boolean): AddTokensToolbarState {
        return if (hasEditAccess) {
            AddTokensToolbarState.Title.EditAccess(
                titleResId = R.string.main_manage_tokens,
                onBackButtonClick = router::popBackStack,
                onSearchButtonClick = ::onSearchButtonClick,
                onAddCustomTokenClick = router::openAddCustomTokenScreen,
            )
        } else {
            AddTokensToolbarState.Title.ReadAccess(
                titleResId = R.string.search_tokens_title,
                onBackButtonClick = router::popBackStack,
                onSearchButtonClick = ::onSearchButtonClick,
            )
        }
    }

    private fun onSearchButtonClick() {
        uiState = when (val state = uiState.toolbarState) {
            is AddTokensToolbarState.Title -> {
                uiState.copySealed(
                    toolbarState = AddTokensToolbarState.SearchInputField(
                        onBackButtonClick = uiState.toolbarState.onBackButtonClick,
                        onSearchButtonClick = ::onSearchButtonClick,
                        value = "",
                        onValueChange = ::onValueChange,
                        onCleanButtonClick = { onValueChange(newValue = "") },
                    ),
                )
            }
            is AddTokensToolbarState.SearchInputField -> {
                store.dispatch(TokensAction.SetSearchInput(state.value))
                uiState.copySealed(
                    getToolbarState(store.state.tokensState.allowToAdd),
                )
            }
        }
    }

    private fun onValueChange(newValue: String) {
        val state = requireNotNull(
            value = uiState.toolbarState as? AddTokensToolbarState.SearchInputField,
            lazyMessage = { "Impossible to change value in another state" },
        )

        uiState = uiState.copySealed(toolbarState = state.copy(value = newValue))
    }
}