package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.pay.isVirtualAccountType
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.moneyaccount.model.MoneyAccountEligibility
import com.tangem.domain.moneyaccount.model.MoneyAccountEntryPoint
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GetMoneyAccountEligibilityUseCase(
    private val getMoneyAccountSuitableWalletsUseCase: GetMoneyAccountSuitableWalletsUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val deviceSecurityInfoProvider: DeviceSecurityInfoProvider,
) {
    suspend operator fun invoke(entryPoint: MoneyAccountEntryPoint?): MoneyAccountEligibility {
        if (deviceSecurityInfoProvider.isSecurityExposed()) {
            return MoneyAccountEligibility.NotAvailable
        }

        val suitableWallets = getMoneyAccountSuitableWalletsUseCase()
        if (suitableWallets.isEmpty()) {
            return MoneyAccountEligibility.NotAvailable
        }

        val isEligible = checkEligibility(entryPoint)
        if (isEligible) {
            return MoneyAccountEligibility.Available(suitableWallets)
        }

        val eligibleWallets = coroutineScope {
            suitableWallets
                .map { wallet ->
                    async {
                        val isExistingCustomer = onboardingRepository.hasTangemPayInWallet(wallet.walletId).getOrNull()
                        wallet.takeIf { isExistingCustomer == true }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }

        return if (eligibleWallets.isEmpty()) {
            MoneyAccountEligibility.NotAvailable
        } else {
            MoneyAccountEligibility.Available(eligibleWallets)
        }
    }

    private suspend fun checkEligibility(entryPoint: MoneyAccountEntryPoint?): Boolean {
        val eligibility = onboardingRepository.getCustomerEligibility()
            ?: onboardingRepository.checkCustomerEligibility()
        return if (entryPoint == null) {
            eligibility.any { it.isVirtualAccountType }
        } else {
            eligibility.contains(entryPoint.toEligibilityType())
        }
    }

    private fun MoneyAccountEntryPoint.toEligibilityType(): TangemPayEligibilityType = when (this) {
        MoneyAccountEntryPoint.BANNER -> TangemPayEligibilityType.BANNER_VIRTUAL_ACCOUNT
        MoneyAccountEntryPoint.DETAILS -> TangemPayEligibilityType.DETAILS_VIRTUAL_ACCOUNT
        MoneyAccountEntryPoint.DEEPLINK -> TangemPayEligibilityType.DEEPLINK_VIRTUAL_ACCOUNT
    }
}