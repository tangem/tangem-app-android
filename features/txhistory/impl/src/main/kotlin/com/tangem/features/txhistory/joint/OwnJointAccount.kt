package com.tangem.features.txhistory.joint

import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun SingleAccountListSupplier.ownJointAccount(
    userWalletId: UserWalletId,
    currency: CryptoCurrency,
): Flow<Account.Joint?> = invoke(userWalletId = userWalletId)
    .map { accountList ->
        accountList.accounts
            .filterIsInstance<Account.Joint>()
            .firstOrNull { account -> account.cryptoCurrencies.any { it.network == currency.network } }
    }
    .distinctUntilChanged()