package com.tangem.features.feed.components.search

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.search.model.UserAssetSearchEntry
import com.tangem.features.feed.model.search.SearchTokenSelectorModel
import com.tangem.features.feed.ui.search.components.TokenSelectorBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class SearchTokenSelectorComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : AppComponentContext by context, ComposableBottomSheetComponent {

    private val model = getOrCreateModel<SearchTokenSelectorModel, Params>(params = params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state = model.state.collectAsStateWithLifecycle()
        TokenSelectorBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = state.value,
            ),
        )
    }

    data class Params(
        val entries: List<UserAssetSearchEntry>,
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
        val onTokenSelected: (UserAssetSearchEntry) -> Unit,
        val onDismiss: () -> Unit,
    )
}