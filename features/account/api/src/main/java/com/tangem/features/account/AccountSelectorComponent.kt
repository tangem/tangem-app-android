package com.tangem.features.account

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.account.Account
import kotlinx.coroutines.flow.StateFlow

interface AccountSelectorComponent : ComposableBottomSheetComponent {

    data class Params(
        val onDismiss: () -> Unit,
        val accountsBalanceFetcher: AccountsBalanceFetcher,
        val controller: AccountSelectorController,
    )

    interface Factory {
        fun create(appComponentContext: AppComponentContext, params: Params): AccountSelectorComponent
    }
}

interface AccountSelectorController {
    val selectedAccount: StateFlow<Account?>
    fun selectAccount(account: Account?)
}