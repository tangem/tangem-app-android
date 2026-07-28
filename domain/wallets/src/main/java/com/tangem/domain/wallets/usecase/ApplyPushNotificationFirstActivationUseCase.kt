package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first

/**
 * First-activation rule: on the first push permission grant, enables all three categories for a wallet.

 * on the backend yet) is retried by the next trigger — wallet screen or notification settings screen.
 */
class ApplyPushNotificationFirstActivationUseCase(
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val preferencesRepository: WalletPushNotificationPreferencesRepository,
    private val notificationsRepository: NotificationsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Unit> {
        return applyRule(userWalletId).onLeft {
            TangemLogger.e("Push first activation failed for $userWalletId", it)
        }
    }

    private suspend fun applyRule(userWalletId: UserWalletId): Either<Throwable, Unit> {
        if (preferencesRepository.isFirstActivationDone(userWalletId)) return Unit.right()

        // Wallets auto-enabled by the legacy flow are adopted as activated to not force-enable extra categories.
        if (userWalletId.stringValue in notificationsRepository.getWalletAutomaticallyEnabledList()) {
            preferencesRepository.markFirstActivationDone(userWalletId)
            return Unit.right()
        }

        // The wallet may not be created on the backend yet (onboarding) — probe before any side effects.
        val probe = Either.catch { preferencesRepository.observePreferences(userWalletId).first() }
        if (probe is Either.Left) return probe.value.left()

        // Tokens (address re-subscription) first, then preferences.
        val tokensResult = setNotificationsEnabledUseCase(userWalletId, isEnabled = true)
        if (tokensResult is Either.Left) return tokensResult

        return preferencesRepository.setAllPreferences(
            userWalletId = userWalletId,
            transactionAlerts = true,
            offersUpdates = true,
            priceAlerts = true,
        )
            .onRight { preferencesRepository.markFirstActivationDone(userWalletId) }
            .onLeft { setNotificationsEnabledUseCase(userWalletId, isEnabled = false) }
    }
}