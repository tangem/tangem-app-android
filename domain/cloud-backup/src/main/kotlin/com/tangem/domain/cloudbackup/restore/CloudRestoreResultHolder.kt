package com.tangem.domain.cloudbackup.restore

import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands the resolved cloud backups from the screen that listed them over to the restore screen: the two sit in
 * different Decompose graphs, and the route between them is serializable, so the result cannot travel through
 * navigation.
 */
@Singleton
class CloudRestoreResultHolder @Inject constructor() {

    val result: StateFlow<CloudRestoreResult?>
        field = MutableStateFlow<CloudRestoreResult?>(null)

    fun set(value: CloudRestoreResult) {
        result.value = value
    }

    fun clear() {
        result.value = null
    }
}

data class CloudRestoreResult(
    val backups: List<CloudBackupInfo>,
    val accountEmail: String?,
)