package com.tangem.feature.wallet.presentation.account

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.supplier.SingleAccountStatusSupplier
import com.tangem.domain.account.status.utils.ExpandedAccountsHolder
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import javax.inject.Inject

@ModelScoped
internal class AccountDependencies @Inject constructor(
    val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    val expandedAccountsHolder: ExpandedAccountsHolder,
    val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    val singleAccountStatusSupplier: SingleAccountStatusSupplier,
)