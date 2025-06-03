package com.tangem.domain.notifications

import arrow.core.Either
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.repository.NotificationsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GetApplicationIdUseCase(
    private val notificationsRepository: NotificationsRepository,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): Either<Throwable, ApplicationId> = Either.catch {
        val localApplicationId = notificationsRepository.getApplicationId()
        if (localApplicationId != null) return@catch localApplicationId

        mutex.withLock {
            val doubleCheckedId = notificationsRepository.getApplicationId()
            if (doubleCheckedId != null) return@withLock doubleCheckedId

            val newApplicationId = notificationsRepository.createApplicationId()
            notificationsRepository.saveApplicationId(newApplicationId)
            newApplicationId
        }
    }
}