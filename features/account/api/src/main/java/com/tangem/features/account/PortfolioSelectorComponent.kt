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

interface PortfolioSelectorComponent : ComposableBottomSheetComponent, ComposableContentComponent {

    val title: StateFlow<TextReference>

    data class Params(
        val onDismiss: () -> Unit,
        val portfolioFetcher: PortfolioFetcher,
        val controller: PortfolioSelectorController,
    )

    interface Factory : ComponentFactory<Params, PortfolioSelectorComponent>
}

/**
 * if [isAccountMode] is false it's mean [selectedAccount] emit [AccountId] for Main account
 */
interface PortfolioSelectorController {
    val isAccountMode: Flow<Boolean>
    val selectedAccount: Flow<AccountId?>
    val selectedAccountSync: AccountId?

    /**
     * for some Feature specific filtering
     */
    var isEnabled: MutableStateFlow<(UserWallet, AccountStatus) -> Boolean>

    fun selectAccount(accountId: AccountId?)
    fun selectedAccountWithData(portfolioFetcher: PortfolioFetcher): Flow<Pair<UserWallet, AccountStatus>?>
}