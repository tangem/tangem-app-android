package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.derivation.isMoneyAccountReservedPath

class IsMoneyAccountPathOccupiedUseCase(
    private val singleAccountListSupplier: SingleAccountListSupplier,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Boolean {
        val currencies = singleAccountListSupplier.getSyncOrNull(userWalletId)?.flattenCurrencies().orEmpty()
        return currencies.any { currency ->
            currency.network.derivationPath.value?.let(::isMoneyAccountReservedPath) == true
        }
    }
}