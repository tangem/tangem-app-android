package com.tangem.features.account.selector

import com.tangem.domain.models.account.Account
import com.tangem.features.account.AccountSelectorController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

internal class DefaultAccountSelectorController @Inject constructor() : AccountSelectorController {

    private val _selectedAccount: MutableStateFlow<Account?> = MutableStateFlow(null)
    override val selectedAccount: StateFlow<Account?> get() = _selectedAccount

    override fun selectAccount(account: Account?) {
        _selectedAccount.update { account }
    }
}