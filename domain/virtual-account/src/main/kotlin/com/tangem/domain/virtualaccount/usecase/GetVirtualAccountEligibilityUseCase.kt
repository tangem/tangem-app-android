package com.tangem.domain.virtualaccount.usecase

import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.pay.isVirtualAccountType
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.virtualaccount.model.VirtualAccountEligibility
import com.tangem.domain.virtualaccount.model.VirtualAccountEntryPoint
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GetVirtualAccountEligibilityUseCase(
    private val getVirtualAccountSuitableWalletsUseCase: GetVirtualAccountSuitableWalletsUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val deviceSecurityInfoProvider: DeviceSecurityInfoProvider,
) {
    suspend operator fun invoke(entryPoint: VirtualAccountEntryPoint?): VirtualAccountEligibility {
        if (deviceSecurityInfoProvider.isSecurityExposed()) {
            return VirtualAccountEligibility.NotAvailable
        }

        val suitableWallets = getVirtualAccountSuitableWalletsUseCase()
        if (suitableWallets.isEmpty()) {
            return VirtualAccountEligibility.NotAvailable
        }

        val isEligible = checkEligibility(entryPoint)
        if (isEligible) {
            return VirtualAccountEligibility.Available(suitableWallets)
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
            VirtualAccountEligibility.NotAvailable
        } else {
            VirtualAccountEligibility.Available(eligibleWallets)
        }
    }

    private suspend fun checkEligibility(entryPoint: VirtualAccountEntryPoint?): Boolean {
        val eligibility = onboardingRepository.getCustomerEligibility().ifEmpty {
            onboardingRepository.checkCustomerEligibility()
        }
        return if (entryPoint == null) {
            eligibility.any { it.isVirtualAccountType }
        } else {
            eligibility.contains(entryPoint.toEligibilityType())
        }
    }

    private fun VirtualAccountEntryPoint.toEligibilityType(): TangemPayEligibilityType = when (this) {
        VirtualAccountEntryPoint.BANNER -> TangemPayEligibilityType.BANNER_VIRTUAL_ACCOUNT
        VirtualAccountEntryPoint.DETAILS -> TangemPayEligibilityType.DETAILS_VIRTUAL_ACCOUNT
        VirtualAccountEntryPoint.DEEPLINK -> TangemPayEligibilityType.DEEPLINK_VIRTUAL_ACCOUNT
    }
}