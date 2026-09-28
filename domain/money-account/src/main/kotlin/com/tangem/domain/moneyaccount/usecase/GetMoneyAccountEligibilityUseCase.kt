package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountEligibility
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GetMoneyAccountEligibilityUseCase(
    private val getMoneyAccountSuitableWalletsUseCase: GetMoneyAccountSuitableWalletsUseCase,
    private val getMoneyAccountStatusUseCase: GetMoneyAccountStatusUseCase,
    private val deviceSecurityInfoProvider: DeviceSecurityInfoProvider,
) {
    suspend operator fun invoke(): MoneyAccountEligibility {
        if (deviceSecurityInfoProvider.isSecurityExposed()) {
            return MoneyAccountEligibility.NotAvailable
        }

        val eligibleWallets = coroutineScope {
            getMoneyAccountSuitableWalletsUseCase()
                .map { wallet -> async { wallet.takeIf { canOpenMoneyAccount(it.walletId) } } }
                .awaitAll()
                .filterNotNull()
        }

        return if (eligibleWallets.isEmpty()) {
            MoneyAccountEligibility.NotAvailable
        } else {
            MoneyAccountEligibility.Available(eligibleWallets)
        }
    }

    private suspend fun canOpenMoneyAccount(userWalletId: UserWalletId): Boolean {
        val status = getMoneyAccountStatusUseCase(userWalletId).getOrNull() ?: return false
        return status is MoneyAccountStatusValue.NotCreated
    }
}