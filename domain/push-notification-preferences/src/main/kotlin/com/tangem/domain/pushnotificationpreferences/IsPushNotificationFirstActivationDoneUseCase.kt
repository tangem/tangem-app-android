package com.tangem.domain.pushnotificationpreferences

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository

class IsPushNotificationFirstActivationDoneUseCase(
    private val repository: WalletPushNotificationPreferencesRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Boolean = repository.isFirstActivationDone(userWalletId)
}