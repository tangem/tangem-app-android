package com.tangem.domain.settings

import arrow.core.Either
import com.tangem.domain.settings.repositories.PermissionRepository

class ShouldInitiallyAskPermissionUseCase(private val repository: PermissionRepository) {

    suspend operator fun invoke(permission: Int): Either<Throwable, Boolean> = Either.catch {
        repository.shouldInitiallyShowPermissionScreen(permission)
    }
}
