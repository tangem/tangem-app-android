package com.tangem.data.wallets

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.data.wallets.converters.UserWalletRemoteInfoConverter
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException
import com.tangem.datasource.api.common.response.fold
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.converters.WalletIdBodyConverter
import com.tangem.datasource.api.tangemTech.models.*
import com.tangem.datasource.api.tangemTech.models.SeedPhraseNotificationDTO.Status
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.PreferencesKeys.SEED_FIRST_NOTIFICATION_SHOW_TIME
import com.tangem.datasource.local.preferences.utils.*
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.SeedPhraseNotificationsStatus
import com.tangem.domain.wallets.models.UserWalletRemoteInfo
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.WEEK_MILLIS
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias SeedPhraseNotificationsStatuses = Map<UserWalletId, SeedPhraseNotificationsStatus>

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
internal class DefaultWalletsRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val seedPhraseNotificationVisibilityStore: RuntimeStateStore<SeedPhraseNotificationsStatuses>,
    private val dispatchers: CoroutineDispatcherProvider,
    private val authProvider: AuthProvider,
    private val walletServerBinder: WalletServerBinder,
    private val appsFlyerStore: AppsFlyerStore,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val moshi: com.squareup.moshi.Moshi,
) : WalletsRepository {

    @Deprecated("Hot wallet feature makes app always save user wallets. Do not use this method")
    override suspend fun shouldSaveUserWalletsSync(): Boolean {
        return appPreferencesStore.getSyncOrDefault(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, default = false)
    }

    @Deprecated("Hot wallet feature makes app always save user wallets. Do not use this method")
    override fun shouldSaveUserWallets(): Flow<Boolean> {
        return appPreferencesStore.get(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, default = false)
    }

    @Deprecated("Hot wallet feature makes app always save user wallets. Do not use this method")
    override suspend fun saveShouldSaveUserWallets(item: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, value = item)
    }

    override suspend fun useBiometricAuthentication(): Boolean {
        val shouldUseBiometricAuth = appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.USE_BIOMETRIC_AUTHENTICATION_KEY,
        )

        if (shouldUseBiometricAuth != null) {
            return shouldUseBiometricAuth
        }

        val isLegacySaveWalletsInTheApp = appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.SAVE_USER_WALLETS_KEY,
        )

        if (isLegacySaveWalletsInTheApp != null) {
            // Migrate legacy setting to new one
            appPreferencesStore.store(
                key = PreferencesKeys.USE_BIOMETRIC_AUTHENTICATION_KEY,
                value = isLegacySaveWalletsInTheApp,
            )
            return isLegacySaveWalletsInTheApp
        } else {
            // Default value for new users
            setUseBiometricAuthentication(false)
            return false
        }
    }

    override suspend fun setUseBiometricAuthentication(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.USE_BIOMETRIC_AUTHENTICATION_KEY, value = value)
    }

    override suspend fun requireAccessCode(): Boolean {
        val isRequireAccessCode = appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.REQUIRE_ACCESS_CODE_KEY,
        )

        if (isRequireAccessCode != null) {
            return isRequireAccessCode
        }

        val isLegacyShouldSaveAccessCode = appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.SHOULD_SAVE_ACCESS_CODES_KEY,
        )

        if (isLegacyShouldSaveAccessCode != null) {
            // Migrate legacy setting to new one
            appPreferencesStore.store(
                key = PreferencesKeys.REQUIRE_ACCESS_CODE_KEY,
                value = isLegacyShouldSaveAccessCode.not(),
            )
            return isLegacyShouldSaveAccessCode.not()
        } else {
            // Default value for new users
            setRequireAccessCode(true)
            return true
        }
    }

    override suspend fun setRequireAccessCode(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.REQUIRE_ACCESS_CODE_KEY, value = value)
    }

    override suspend fun isWalletWithRing(userWalletId: UserWalletId): Boolean {
        return appPreferencesStore
            .getSyncOrDefault(key = PreferencesKeys.ADDED_WALLETS_WITH_RING_KEY, default = emptySet())
            .contains(element = userWalletId.stringValue)
    }

    override suspend fun setHasWalletsWithRing(userWalletId: UserWalletId) {
        appPreferencesStore.editData { mutablePreferences ->
            val added = mutablePreferences[PreferencesKeys.ADDED_WALLETS_WITH_RING_KEY].orEmpty()

            mutablePreferences[PreferencesKeys.ADDED_WALLETS_WITH_RING_KEY] = added + userWalletId.stringValue
        }
    }

    override fun seedPhraseNotificationStatus(userWalletId: UserWalletId): Flow<SeedPhraseNotificationsStatus> {
        return channelFlow {
            launch {
                seedPhraseNotificationVisibilityStore.get()
                    .map { map ->
                        map.getOrDefault(
                            key = userWalletId,
                            defaultValue = SeedPhraseNotificationsStatus.NOT_NEEDED,
                        )
                    }
                    .collectLatest(::send)
            }

            fetchSeedPhraseNotificationStatus(userWalletId)
        }
    }

    private suspend fun fetchSeedPhraseNotificationStatus(userWalletId: UserWalletId) {
        val userWallet = userWalletsStore.getSyncOrNull(key = userWalletId)

        if (userWallet != null && userWallet !is UserWallet.Cold) {
            updateNotificationVisibility(id = userWalletId, value = SeedPhraseNotificationsStatus.NOT_NEEDED)
            return
        }

        val status = if (userWallet?.isImported == false) {
            Status.NOT_NEEDED
        } else {
            runCatching(dispatchers.io) {
                tangemTechApi.getSeedPhraseNotificationStatus(walletId = userWalletId.stringValue).getOrThrow()
            }.fold(
                onSuccess = { it.status },
                onFailure = { throwable ->
                    if (throwable is HttpException && throwable.code == HttpException.Code.NOT_FOUND) {
                        Status.NOTIFIED
                    } else {
                        Status.NOT_NEEDED
                    }
                },
            )
        }

        when {
            status == Status.NOTIFIED -> updateNotificationVisibility(
                id = userWalletId,
                value = SeedPhraseNotificationsStatus.SHOW_FIRST,
            )
            status == Status.CONFIRMED && checkNeedFetchSecondNotification() ->
                fetchSeedPhraseSecondNotificationStatus(userWalletId)
            else -> updateNotificationVisibility(id = userWalletId, value = SeedPhraseNotificationsStatus.NOT_NEEDED)
        }
    }

    private suspend fun fetchSeedPhraseSecondNotificationStatus(userWalletId: UserWalletId) {
        val status = runCatching(dispatchers.io) {
            tangemTechApi.getSeedPhraseSecondNotificationStatus(walletId = userWalletId.stringValue).getOrThrow()
        }.fold(
            onSuccess = { it.status },
            onFailure = { Status.NOT_NEEDED },
        )

        val showStatus = when (status) {
            Status.CONFIRMED -> SeedPhraseNotificationsStatus.SHOW_SECOND
            else -> SeedPhraseNotificationsStatus.NOT_NEEDED
        }

        updateNotificationVisibility(id = userWalletId, value = showStatus)
    }

    private suspend fun checkNeedFetchSecondNotification(): Boolean {
        val firstNotificationTime =
            appPreferencesStore.getSyncOrDefault(SEED_FIRST_NOTIFICATION_SHOW_TIME, default = 0L)
        return System.currentTimeMillis() - firstNotificationTime > WEEK_MILLIS
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
            appPreferencesStore.store(key = SEED_FIRST_NOTIFICATION_SHOW_TIME, value = System.currentTimeMillis())
        }

        updateNotificationVisibility(id = userWalletId, value = SeedPhraseNotificationsStatus.NOT_NEEDED)
    }

    override suspend fun declineSeedPhraseNotification(userWalletId: UserWalletId) {
        runCatching(dispatchers.io) {
            tangemTechApi.updateSeedPhraseNotificationStatus(
                walletId = userWalletId.stringValue,
                body = SeedPhraseNotificationDTO(status = Status.DECLINED),
            ).getOrThrow()
            appPreferencesStore.store(key = SEED_FIRST_NOTIFICATION_SHOW_TIME, value = System.currentTimeMillis())
        }

        updateNotificationVisibility(id = userWalletId, value = SeedPhraseNotificationsStatus.NOT_NEEDED)
    }

    override suspend fun createWallet(userWalletId: UserWalletId) {
        walletServerBinder.bind(userWalletId)
    }

    override suspend fun rejectSeedPhraseSecondNotification(userWalletId: UserWalletId) {
        runCatching(dispatchers.io) {
            tangemTechApi.updateSeedPhraseSecondNotificationStatus(
                walletId = userWalletId.stringValue,
                body = SeedPhraseNotificationDTO(status = Status.REJECTED),
            ).getOrThrow()
        }

        updateNotificationVisibility(id = userWalletId, value = SeedPhraseNotificationsStatus.NOT_NEEDED)
    }

    override suspend fun acceptSeedPhraseSecondNotification(userWalletId: UserWalletId) {
        runCatching(dispatchers.io) {
            tangemTechApi.updateSeedPhraseSecondNotificationStatus(
                walletId = userWalletId.stringValue,
                body = SeedPhraseNotificationDTO(status = Status.ACCEPTED),
            ).getOrThrow()
        }

        updateNotificationVisibility(id = userWalletId, value = SeedPhraseNotificationsStatus.NOT_NEEDED)
    }

    private suspend fun updateNotificationVisibility(id: UserWalletId, value: SeedPhraseNotificationsStatus) {
        return seedPhraseNotificationVisibilityStore.update { map ->
            map.toMutableMap().apply {
                this[id] = value
            }
        }
    }

    override fun nftEnabledStatus(userWalletId: UserWalletId): Flow<Boolean> = appPreferencesStore
        .getObjectMap<Boolean>(PreferencesKeys.WALLETS_NFT_ENABLED_STATES_KEY)
        .map { it[userWalletId.stringValue] == true }

    override fun nftEnabledStatuses(): Flow<Map<UserWalletId, Boolean>> = appPreferencesStore
        .getObjectMap<Boolean>(PreferencesKeys.WALLETS_NFT_ENABLED_STATES_KEY)
        .map { map -> map.mapKeys { UserWalletId(it.key) } }

    override suspend fun enableNFT(userWalletId: UserWalletId) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.WALLETS_NFT_ENABLED_STATES_KEY,
                value = mutablePreferences.getObjectMap<Boolean>(PreferencesKeys.WALLETS_NFT_ENABLED_STATES_KEY)
                    .plus(userWalletId.stringValue to true),
            )
        }
    }

    override suspend fun disableNFT(userWalletId: UserWalletId) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.WALLETS_NFT_ENABLED_STATES_KEY,
                value = mutablePreferences.getObjectMap<Boolean>(PreferencesKeys.WALLETS_NFT_ENABLED_STATES_KEY)
                    .plus(userWalletId.stringValue to false),
            )
        }
    }

    override fun notificationsEnabledStatus(userWalletId: UserWalletId): Flow<Boolean> = appPreferencesStore
        .getObjectMap<Boolean>(PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY)
        .map { it[userWalletId.stringValue] == true }

    override suspend fun isNotificationsEnabled(userWalletId: UserWalletId): Boolean =
        appPreferencesStore.getObjectMap<Boolean>(PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY)
            .map { it[userWalletId.stringValue] == true }
            .firstOrNull() == true

    override suspend fun setNotificationsEnabled(userWalletId: UserWalletId, isEnabled: Boolean) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY,
                value = mutablePreferences.getObjectMap<Boolean>(PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY)
                    .plus(userWalletId.stringValue to isEnabled),
            )
        }
    }

    override suspend fun setWalletName(walletId: UserWalletId, walletName: String) = withContext(dispatchers.io) {
        val userWallet = userWalletsStore.getSyncOrNull(key = walletId)

        tangemTechApi.updateWallet(
            walletId = walletId.stringValue,
            body = WalletBody(name = walletName, type = WalletType.from(userWallet)),
        ).getOrThrow()
    }

    override suspend fun upgradeWallet(walletId: UserWalletId) = withContext(dispatchers.io) {
        val userWallet = userWalletsStore.getSyncStrict(key = walletId)

        tangemTechApi.updateWallet(
            walletId = walletId.stringValue,
            body = WalletBody(name = userWallet.name, type = WalletType.from(userWallet)),
        ).getOrThrow()
    }

    override suspend fun getWalletInfo(walletId: UserWalletId): UserWalletRemoteInfo = withContext(dispatchers.io) {
        UserWalletRemoteInfoConverter.convert(
            value = tangemTechApi.getWalletById(walletId.stringValue).getOrThrow(),
        )
    }

    override suspend fun getWalletsInfo(applicationId: String, updateCache: Boolean): List<UserWalletRemoteInfo> =
        withContext(dispatchers.io) {
            tangemTechApi.getWallets(applicationId)
                .getOrThrow()
                .map { walletInfo ->
                    val userWallet = UserWalletRemoteInfoConverter.convert(
                        value = walletInfo,
                    )
                    if (updateCache) {
                        setNotificationsEnabled(
                            userWalletId = userWallet.walletId,
                            isEnabled = userWallet.isNotificationsEnabled,
                        )
                    }
                    userWallet
                }
        }

    override suspend fun associateWallets(applicationId: String, wallets: List<UserWallet>) =
        withContext(dispatchers.io) {
            if (accountsFeatureToggles.isFeatureEnabled) {
                val associateApplicationIdWithWallets: suspend () -> ApiResponse<Unit> = {
                    tangemTechApi.associateApplicationIdWithWalletsV2(
                        applicationId = applicationId,
                        body = AssociateApplicationIdWithWalletsBody(
                            walletIds = wallets.map { it.walletId.stringValue }.distinct(),
                        ),
                    )
                }

                val apiResponse = associateApplicationIdWithWallets()

                if (apiResponse is ApiResponse.Success) return@withContext

                if (apiResponse is ApiResponse.Error &&
                    apiResponse.cause.isNetworkError(HttpException.Code.BAD_REQUEST)
                ) {
                    val errorBody = (apiResponse.cause as? HttpException)?.errorBody
                        ?: error("Bad Request must have error body")

                    val adapter = moshi.adapter(AssociateAppWithWalletsErrorResponse::class.java)
                    val errorResponse = adapter.fromJson(errorBody)
                        ?: error("Cannot parse error body: $errorBody")

                    errorResponse.missingWalletIds
                        .map {
                            async { createWallet(userWalletId = UserWalletId(it)) }
                        }
                        .awaitAll()

                    associateApplicationIdWithWallets().getOrThrow()
                }
            } else {
                val conversionData = appsFlyerStore.get()
                val publicKeys = authProvider.getCardsPublicKeys()
                val walletsBody = wallets.map { userWallet ->
                    WalletIdBodyConverter.convert(
                        userWallet = userWallet,
                        conversionData = conversionData,
                        publicKeys = if (userWallet is UserWallet.Cold) {
                            publicKeys.filterKeys {
                                userWallet.cardsInWallet.contains(it)
                            }
                        } else {
                            emptyMap()
                        },
                    )
                }

                tangemTechApi.associateApplicationIdWithWallets(
                    applicationId = applicationId,
                    body = walletsBody,
                ).getOrThrow()
            }
        }

    override suspend fun activatePromoCode(
        promoCode: String,
        bitcoinAddress: String,
    ): Either<ActivatePromoCodeError, String> = withContext(dispatchers.io) {
        tangemTechApi.activatePromoCode(
            body = PromocodeActivationBody(
                promoCode = promoCode,
                address = bitcoinAddress,
            ),
        ).fold(
            onSuccess = { it.status.right() },
            onError = { apiResponseError ->
                val error = when (apiResponseError) {
                    is HttpException -> when (apiResponseError.code) {
                        HttpException.Code.NOT_FOUND -> ActivatePromoCodeError.InvalidPromoCode
                        HttpException.Code.CONFLICT -> ActivatePromoCodeError.PromocodeAlreadyUsed
                        else -> ActivatePromoCodeError.ActivationFailed
                    }
                    else -> ActivatePromoCodeError.ActivationFailed
                }

                error.left()
            },
        )
    }
}