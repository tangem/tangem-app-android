package com.tangem.features.tangempay.eligibility.onboarding

import com.tangem.domain.models.wallet.UserWalletId

internal interface OnboardingLauncher {

    suspend fun start(
        userWalletId: UserWalletId?,
        onWalletSelectionRequired: (List<UserWalletId>) -> Unit,
        onFailed: () -> Unit,
    )

    suspend fun continueWith(userWalletId: UserWalletId, onFailed: () -> Unit)
}