package com.tangem.domain.pushnotificationpreferences

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository

class SetAllWalletPushNotificationPreferencesUseCase(
    private val repository: WalletPushNotificationPreferencesRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        transactionAlerts: Boolean,
        offersUpdates: Boolean,
        priceAlerts: Boolean,
    ): Either<Throwable, Unit> = repository.setAllPreferences(
        userWalletId = userWalletId,
        transactionAlerts = transactionAlerts,
        offersUpdates = offersUpdates,
        priceAlerts = priceAlerts,
    )
}