package com.tangem.domain.pushnotificationpreferences

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveWalletPushNotificationPreferencesUseCase(
    private val repository: WalletPushNotificationPreferencesRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<WalletPushNotificationPreferences> =
        repository.observePreferences(userWalletId)
}