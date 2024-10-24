package com.tangem.feature.wallet.presentation.tokenlist.entity

import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.TokensListItemState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

/**
 * [TokenListUM] controller
 *
[REDACTED_AUTHOR]
 */
internal class TokenListUMController @Inject constructor() {

    val state: StateFlow<TokenListUM> get() = _state

    private val _state: MutableStateFlow<TokenListUM> = MutableStateFlow(
        value = TokenListUM(
            items = persistentListOf(),
            isBalanceHidden = false,
        ),
    )

    fun update(transform: (TokenListUM) -> TokenListUM) {
        Timber.d("Applying non-name transformation")
        _state.update(transform)
    }

    fun update(transformer: TokenListUMTransformer) {
        Timber.d("Applying ${transformer::class.simpleName}")
        _state.update(transformer::transform)
    }

    /** Get search bar if it exists */
    fun getSearchBar(): TokensListItemState.SearchBar? {
        return _state.value.getSearchBar()
    }
}