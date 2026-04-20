package com.tangem.features.feed.model.search

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.feed.components.search.SearchTokenSelectorComponent
import com.tangem.features.feed.model.search.state.TokenSelectorStateController
import com.tangem.features.feed.model.search.state.transformers.BuildTokenSelectorSectionsTransformer
import com.tangem.features.feed.ui.search.state.TokenSelectorContentUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class SearchTokenSelectorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val stateController: TokenSelectorStateController,
) : Model() {

    private val params = paramsContainer.require<SearchTokenSelectorComponent.Params>()

    val state: StateFlow<TokenSelectorContentUM>
        get() = stateController.uiState

    init {
        stateController.update(
            BuildTokenSelectorSectionsTransformer(
                entries = params.entries,
                appCurrency = params.appCurrency,
                isBalanceHidden = params.isBalanceHidden,
                onTokenSelected = params.onTokenSelected,
            ),
        )
    }
}