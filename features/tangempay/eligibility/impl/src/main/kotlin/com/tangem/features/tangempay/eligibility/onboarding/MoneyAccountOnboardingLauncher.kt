package com.tangem.features.tangempay.eligibility.onboarding

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountEligibility
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountEligibilityUseCase
import javax.inject.Inject

@ModelScoped
internal class MoneyAccountOnboardingLauncher @Inject constructor(
    private val router: Router,
    private val getMoneyAccountEligibilityUseCase: GetMoneyAccountEligibilityUseCase,
) : OnboardingLauncher {

    override suspend fun start(
        userWalletId: UserWalletId?,
        onWalletSelectionRequired: (List<UserWalletId>) -> Unit,
        onFailed: () -> Unit,
    ) {
        if (userWalletId != null) {
            continueWith(userWalletId = userWalletId, onFailed = onFailed)
            return
        }

        val walletsIds = when (val eligibility = getMoneyAccountEligibilityUseCase()) {
            is MoneyAccountEligibility.Available -> eligibility.wallets.map { it.walletId }
            MoneyAccountEligibility.NotAvailable -> emptyList()
        }

        when (walletsIds.size) {
            0 -> router.pop()
            1 -> continueWith(
                userWalletId = walletsIds.first(),
                onFailed = onFailed,
            )
            else -> onWalletSelectionRequired(walletsIds)
        }
    }

    // TODO v_rodionov: #[REDACTED_TASK_KEY] derive and sign deposit address, create order and start KYC
    override suspend fun continueWith(userWalletId: UserWalletId, onFailed: () -> Unit) {
        router.pop()
    }
}