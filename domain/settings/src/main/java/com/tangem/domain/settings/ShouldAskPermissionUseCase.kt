package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.PermissionRepository

class ShouldAskPermissionUseCase(
    private val repository: PermissionRepository,
) {

    suspend operator fun invoke(permission: Int): Boolean = repository.shouldAskPermission(permission)
}