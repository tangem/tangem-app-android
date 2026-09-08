package com.tangem.data.collectibles.account

import com.tangem.data.collectibles.account.converter.CollectiblesAccountStatusValueDTOConverter
import com.tangem.data.collectibles.account.entity.CollectiblesAccountStatusValueDTO
import com.tangem.data.collectibles.CollectiblesPreferencesKeys
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.isAccountCreated
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Total by design: every operation degrades to "nothing recorded" instead of throwing. The record only ever saves
 * a chain read, so losing it costs one — while a throw here would surface as a crash on opening the feature, or as
 * a failed report for an onboarding that actually succeeded.
 *
 * What reaches the disk is [CollectiblesAccountStatusValueDTO], not the domain type — see its documentation for
 * which states are persisted at all, and why the status source is not among them.
 */
internal class DefaultCollectiblesAccountRepository @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
) : CollectiblesAccountRepository {

    override fun getStatus(userWalletId: UserWalletId): Flow<CollectiblesAccountStatusValue?> = appPreferencesStore
        .data
        .map { preferences -> preferences[key(userWalletId)]?.toStatus() }
        .distinctUntilChanged()
        .catch { error ->
            TangemLogger.e("Failed to observe the collectibles account of $userWalletId", error)
            emit(null)
        }

    override suspend fun getStatusSync(userWalletId: UserWalletId): CollectiblesAccountStatusValue? =
        runSuspendCatching { appPreferencesStore.data.first()[key(userWalletId)]?.toStatus() }
            .onFailure { TangemLogger.e("Failed to read the collectibles account of $userWalletId", it) }
            .getOrNull()

    override suspend fun saveAccount(userWalletId: UserWalletId, status: CollectiblesAccountStatusValue) {
        val stored = CollectiblesAccountStatusValueDTOConverter.convert(status)

        if (stored == null) {
            TangemLogger.e("Refusing to record $status as a collectibles account of $userWalletId")
            return
        }

        // TODO([REDACTED_TASK_KEY]): a wallet with nothing recorded is onboarding for the first time, so this is where the
        //  account is registered with the backend — `PUT /accounts` with a `collectibles` record. Until the
        //  backend knows the type, the account lives on this device only.
        if (!getStatusSync(userWalletId).isAccountCreated) {
            TangemLogger.d(messageString = "Collectibles account of $userWalletId created")
        }

        runSuspendCatching {
            appPreferencesStore.editData { preferences -> preferences[key(userWalletId)] = stored.name }
        }.onFailure { TangemLogger.e("Failed to record the collectibles account of $userWalletId", it) }
    }

    override suspend fun clearAccount(userWalletId: UserWalletId) {
        runSuspendCatching {
            appPreferencesStore.editData { preferences -> preferences.remove(key(userWalletId)) }
        }.onFailure { TangemLogger.e("Failed to clear the collectibles account of $userWalletId", it) }
    }

    private fun key(userWalletId: UserWalletId) = CollectiblesPreferencesKeys.getAccountStatusKey(userWalletId)

    /** A record this build cannot read is not an account: it degrades to "nothing recorded". */
    private fun String.toStatus(): CollectiblesAccountStatusValue? = CollectiblesAccountStatusValueDTO.entries
        .firstOrNull { it.name == this }
        ?.let(CollectiblesAccountStatusValueDTOConverter::convertBack)
}