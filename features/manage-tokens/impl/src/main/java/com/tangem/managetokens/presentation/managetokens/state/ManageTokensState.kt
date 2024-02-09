package com.tangem.managetokens.presentation.managetokens.state

import androidx.paging.PagingData
import com.tangem.core.ui.event.StateEvent
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.state.Event
import kotlinx.coroutines.flow.Flow

internal data class ManageTokensState(
    val searchBarState: SearchBarState,
    val tokens: Flow<PagingData<TokenItemState>>,
    val isLoading: Boolean,
    val addCustomTokenButton: AddCustomTokenButton,
    val chooseWalletState: ChooseWalletState,
    val derivationNotification: DerivationNotificationState? = null,
    val selectedToken: TokenItemState.Loaded? = null,
    val showChooseWalletScreen: Boolean = false,
    val event: StateEvent<Event>,
    val onEmptySearchResult: (String) -> Unit,
)

data class AddCustomTokenButton(
    val isVisible: Boolean,
    val onClick: () -> Unit,
)