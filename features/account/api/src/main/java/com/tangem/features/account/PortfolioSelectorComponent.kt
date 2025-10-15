package com.tangem.features.account

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * How to use
 * 1) Create and keep instance of [PortfolioFetcher] and [PortfolioSelectorController] in your Feature Model
 * 2) Provide them via [Params]
 * 3) Now you have a bridge between your Feature and PortfolioSelector
 * 4) Initial state is unselected. Select yourself [PortfolioSelectorController.selectAccount]
 * or offer users to select
 *
 * 5) Listen [PortfolioSelectorController.selectedAccount] or [PortfolioSelectorController.selectedAccountWithData]
 *
 * Note:
 *  - Supports [PortfolioSelectorComponent.BottomSheet] and [PortfolioSelectorComponent.Content] modes
 */
interface PortfolioSelectorComponent : ComposableBottomSheetComponent, ComposableContentComponent {

    val title: StateFlow<TextReference>

    data class Params(
        val portfolioFetcher: PortfolioFetcher,
        val controller: PortfolioSelectorController,
        val bsCallback: BottomSheetCallback? = null,
    )

    interface BottomSheetCallback {
        val onDismiss: () -> Unit
        val onBack: () -> Unit
    }

    interface Factory : ComponentFactory<Params, PortfolioSelectorComponent>
}

/**
 * How to use see [PortfolioSelectorComponent]
 *
 * if [isAccountMode] is false it's mean [selectedAccount] emit [AccountId] for Main account
 */
interface PortfolioSelectorController {
    val isAccountMode: Flow<Boolean>
    val selectedAccount: Flow<AccountId?>
    val selectedAccountSync: AccountId?

    /**
     * for some Feature specific filtering
     * combine and update with your Feature data and [PortfolioFetcher.data]
     */
    val isEnabled: MutableStateFlow<(UserWallet, AccountStatus) -> Boolean>

    fun selectAccount(accountId: AccountId?)
    fun selectedAccountWithData(portfolioFetcher: PortfolioFetcher): Flow<Pair<UserWallet, AccountStatus>?>
}