package com.tangem.domain.pushnotificationpreferences

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository

class MarkPushNotificationFirstActivationDoneUseCase(
    private val repository: WalletPushNotificationPreferencesRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) = repository.markFirstActivationDone(userWalletId)
}