package com.tangem.feature.wallet.presentation.account

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.supplier.SingleAccountStatusSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.account.status.utils.MainExpandedAccountsHolder
import javax.inject.Inject

@ModelScoped
internal class AccountDependencies @Inject constructor(
    val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    val expandedAccountsHolder: MainExpandedAccountsHolder,
    val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    val singleAccountStatusSupplier: SingleAccountStatusSupplier,
)