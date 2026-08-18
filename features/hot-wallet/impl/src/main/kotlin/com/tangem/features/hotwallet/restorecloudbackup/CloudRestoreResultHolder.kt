package com.tangem.features.hotwallet.restorecloudbackup

import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands the resolved cloud backups from the import bottom sheet over to the restore screen: the two sit
 * in different Decompose graphs, and the route between them is serializable, so the result cannot travel
 * through navigation.
 */
@Singleton
internal class CloudRestoreResultHolder @Inject constructor() {

    val result: StateFlow<CloudRestoreResult?>
        field = MutableStateFlow<CloudRestoreResult?>(null)

    fun set(value: CloudRestoreResult) {
        result.value = value
    }

    fun clear() {
        result.value = null
    }
}

internal data class CloudRestoreResult(
    val backups: List<CloudBackupInfo>,
    val accountEmail: String?,
)