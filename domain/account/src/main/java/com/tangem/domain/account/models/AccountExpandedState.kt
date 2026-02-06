package com.tangem.domain.account.models

import com.tangem.domain.models.account.AccountId

data class AccountExpandedState(
    val accountId: AccountId,
    val isExpanded: Boolean,
)