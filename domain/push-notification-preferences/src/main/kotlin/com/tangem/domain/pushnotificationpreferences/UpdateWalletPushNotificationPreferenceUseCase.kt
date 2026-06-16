package com.tangem.domain.pushnotificationpreferences

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationCategory
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository

class UpdateWalletPushNotificationPreferenceUseCase(
    private val repository: WalletPushNotificationPreferencesRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        category: PushNotificationCategory,
        isEnabled: Boolean,
    ): Either<Throwable, Unit> = repository.updatePreference(
        userWalletId = userWalletId,
        category = category,
        isEnabled = isEnabled,
    )
}