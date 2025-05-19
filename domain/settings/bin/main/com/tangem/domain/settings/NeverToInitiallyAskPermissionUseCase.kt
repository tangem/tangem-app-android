package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.PermissionRepository

class NeverToInitiallyAskPermissionUseCase(
    private val repository: PermissionRepository,
) {

    suspend operator fun invoke(permission: String) {
        repository.neverInitiallyShowPermissionScreen(permission)
    }
}