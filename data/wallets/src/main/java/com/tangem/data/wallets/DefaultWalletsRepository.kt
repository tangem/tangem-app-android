package com.tangem.data.wallets

import com.tangem.datasource.api.common.response.ApiResponseError.HttpException
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.MarkUserWalletWasCreatedBody
import com.tangem.datasource.api.tangemTech.models.SeedPhraseNotificationDTO
import com.tangem.datasource.api.tangemTech.models.SeedPhraseNotificationDTO.Status
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class DefaultWalletsRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val seedPhraseNotificationVisibilityStore: RuntimeStateStore<Map<UserWalletId, Boolean>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletsRepository {

    override suspend fun shouldSaveUserWalletsSync(): Boolean {
        return appPreferencesStore.getSyncOrDefault(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, default = false)
    }

    override fun shouldSaveUserWallets(): Flow<Boolean> {
        return appPreferencesStore.get(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, default = false)
    }

    override suspend fun saveShouldSaveUserWallets(item: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, value = item)
    }

    override suspend fun isWalletWithRing(userWalletId: UserWalletId): Boolean {
        return appPreferencesStore
            .getSyncOrDefault(key = PreferencesKeys.ADDED_WALLETS_WITH_RING_KEY, default = emptySet())
            .contains(element = userWalletId.stringValue)
    }

    override suspend fun setHasWalletsWithRing(userWalletId: UserWalletId) {
        appPreferencesStore.editData {
            val added = it[PreferencesKeys.ADDED_WALLETS_WITH_RING_KEY].orEmpty()

            it[PreferencesKeys.ADDED_WALLETS_WITH_RING_KEY] = added + userWalletId.stringValue
        }
    }

    override fun seedPhraseNotificationStatus(userWalletId: UserWalletId): Flow<Boolean> {
        return channelFlow {
            launch {
                seedPhraseNotificationVisibilityStore.get()
                    .map { it.getOrDefault(key = userWalletId, defaultValue = false) }
                    .collectLatest(::send)
            }

            fetchSeedPhraseNotificationStatus(userWalletId)
        }
    }

    private suspend fun fetchSeedPhraseNotificationStatus(userWalletId: UserWalletId) {
        val userWallet = userWalletsStore.getSyncOrNull(key = userWalletId)

        val status = if (userWallet?.isImported == false) {
            false
        } else {
            runCatching(dispatchers.io) {
                tangemTechApi.getSeedPhraseNotificationStatus(walletId = userWalletId.stringValue).getOrThrow()
            }
                .fold(
                    onSuccess = { it.status == Status.NOTIFIED },
                    onFailure = { it is HttpException && it.code == HttpException.Code.NOT_FOUND },
                )
        }

        updateNotificationVisibility(id = userWalletId, value = status)
    }

    override suspend fun notifiedSeedPhraseNotification(userWalletId: UserWalletId) {
        runCatching(dispatchers.io) {
            tangemTechApi.updateSeedPhraseNotificationStatus(
                walletId = userWalletId.stringValue,
                body = SeedPhraseNotificationDTO(status = Status.NOTIFIED),
            ).getOrThrow()
        }
    }

    override suspend fun confirmSeedPhraseNotification(userWalletId: UserWalletId) {
        runCatching(dispatchers.io) {
            tangemTechApi.updateSeedPhraseNotificationStatus(
                walletId = userWalletId.stringValue,
                body = SeedPhraseNotificationDTO(status = Status.CONFIRMED),
            ).getOrThrow()
        }

        updateNotificationVisibility(id = userWalletId, value = false)
    }

    override suspend fun declineSeedPhraseNotification(userWalletId: UserWalletId) {
        runCatching(dispatchers.io) {
            tangemTechApi.updateSeedPhraseNotificationStatus(
                walletId = userWalletId.stringValue,
                body = SeedPhraseNotificationDTO(status = Status.DECLINED),
            ).getOrThrow()
        }

        updateNotificationVisibility(id = userWalletId, value = false)
    }

    override suspend fun markWallet2WasCreated(userWalletId: UserWalletId) {
        runCatching(dispatchers.io) {
            tangemTechApi.markUserWallerWasCreated(
                body = MarkUserWalletWasCreatedBody(userWalletId = userWalletId.stringValue),
            )
        }
    }

    private suspend fun updateNotificationVisibility(id: UserWalletId, value: Boolean) {
        return seedPhraseNotificationVisibilityStore.update {
            it.toMutableMap().apply {
                this[id] = value
            }
        }
    }
}