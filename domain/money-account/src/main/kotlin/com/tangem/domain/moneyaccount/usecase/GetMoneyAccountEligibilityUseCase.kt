package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.derivation.isMoneyAccountReservedPath
import com.tangem.domain.moneyaccount.model.MoneyAccountEligibility
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GetMoneyAccountEligibilityUseCase(
    private val getMoneyAccountSuitableWalletsUseCase: GetMoneyAccountSuitableWalletsUseCase,
    private val getMoneyAccountStatusUseCase: GetMoneyAccountStatusUseCase,
    private val deviceSecurityInfoProvider: DeviceSecurityInfoProvider,
    private val onboardingRepository: OnboardingRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
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
        if (hasUserCurrencyOnReservedPath(userWalletId)) return false
        if (!onboardingRepository.isTangemPayInitialDataProduced(userWalletId)) return true

        val status = getMoneyAccountStatusUseCase(userWalletId).getOrNull() ?: return false
        return status is MoneyAccountStatusValue.NotCreated
    }

    private suspend fun hasUserCurrencyOnReservedPath(userWalletId: UserWalletId): Boolean {
        val currencies = singleAccountListSupplier.getSyncOrNull(userWalletId)?.flattenCurrencies().orEmpty()
        return currencies.any { currency ->
            currency.network.derivationPath.value?.let(::isMoneyAccountReservedPath) == true
        }
    }
}