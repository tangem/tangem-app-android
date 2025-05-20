package com.tangem.domain.notifications

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.lib.crypto.BlockchainUtils.isTron

class IncrementNotificationsShowCountUseCase(
    private val notificationsRepository: NotificationsRepository,
) {

    suspend operator fun invoke(cryptoCurrency: CryptoCurrency) {
        val isTronToken = cryptoCurrency is CryptoCurrency.Token &&
            isTron(cryptoCurrency.network.rawId)

        if (isTronToken) {
            notificationsRepository.incrementTronTokenFeeNotificationShowCounter()
        }
    }
}