package com.tangem.features.feed.model.search.state

import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.features.feed.model.search.state.transformers.TokenSelectorUMTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class TokenSelectorStateController @Inject constructor() {

    private val mutableUiState: MutableStateFlow<TokenSelectorContentUM> = MutableStateFlow(
        value = TokenSelectorContentUM(sections = persistentListOf()),
    )

    val uiState: StateFlow<TokenSelectorContentUM> get() = mutableUiState.asStateFlow()

    fun update(transformer: TokenSelectorUMTransformer) {
        mutableUiState.update(function = transformer::transform)
    }
}