package com.tangem.domain.settings

import arrow.core.Either
import com.tangem.domain.settings.repositories.PermissionRepository

class IsFirstTimeAskingPermissionUseCase(private val repository: PermissionRepository) {

    suspend operator fun invoke(permission: String): Either<Throwable, Boolean> = Either.catch {
        repository.isFirstTimeAskingPermission(permission)
    }
}