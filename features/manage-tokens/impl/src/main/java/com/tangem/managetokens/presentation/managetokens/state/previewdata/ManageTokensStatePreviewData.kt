package com.tangem.managetokens.presentation.managetokens.state.previewdata

import androidx.paging.PagingData
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.managetokens.presentation.common.state.previewdata.ChooseWalletStatePreviewData
import com.tangem.managetokens.presentation.managetokens.state.*
import com.tangem.managetokens.presentation.managetokens.state.ManageTokensState
import com.tangem.managetokens.presentation.managetokens.state.SearchBarState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import kotlinx.coroutines.flow.flowOf

internal object ManageTokensStatePreviewData {
    val loadedState: ManageTokensState
        get() = ManageTokensState(
            searchBarState = searchState,
            tokens = flowOf(PagingData.from(tokens)),
            isLoading = false,
            addCustomTokenButton = AddCustomTokenButton(true, {}),
            derivationNotification = DerivationNotificationStatePreviewData.state,
            event = consumedEvent(),
            chooseWalletState = ChooseWalletStatePreviewData.state,
            onEmptySearchResult = {},
            customTokenBottomSheetConfig = TangemBottomSheetConfig(false, {}, TangemBottomSheetConfigContent.Empty),
        )

    val loadingState: ManageTokensState
        get() = loadedState.copy(isLoading = true)

    private val tokens: List<TokenItemState>
        get() = listOf(
            TokenItemStatePreviewData.loadedPriceDown,
            TokenItemStatePreviewData.loadedPriceUp,
            TokenItemStatePreviewData.loadedPriceNeutral,
        )

    private val searchState: SearchBarState
        get() = SearchBarState(
            query = "",
            onQueryChange = {},
            active = false,
            onActiveChange = {},
        )
}