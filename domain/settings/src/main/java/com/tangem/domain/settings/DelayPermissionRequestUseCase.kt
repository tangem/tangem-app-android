package com.tangem.domain.settings

import arrow.core.Either
import com.tangem.domain.settings.repositories.PermissionRepository

class DelayPermissionRequestUseCase(
    private val repository: PermissionRepository,
) {

    suspend operator fun invoke(permission: Int): Either<Throwable, Unit> = Either.catch {
        repository.delayPermissionAsking(permission)
    }
}