package com.tangem.domain.notifications

import arrow.core.Either
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.repository.PushNotificationsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GetApplicationIdUseCase(
    private val pushNotificationsRepository: PushNotificationsRepository,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): Either<Throwable, ApplicationId> = Either.catch {
        val localApplicationId = pushNotificationsRepository.getApplicationId()
        if (localApplicationId != null) return@catch localApplicationId

        mutex.withLock {
            val doubleCheckedId = pushNotificationsRepository.getApplicationId()
            if (doubleCheckedId != null) return@withLock doubleCheckedId

            val newApplicationId = pushNotificationsRepository.createApplicationId()
            pushNotificationsRepository.saveApplicationId(newApplicationId)
            newApplicationId
        }
    }
}