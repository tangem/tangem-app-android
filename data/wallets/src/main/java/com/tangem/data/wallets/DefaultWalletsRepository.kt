package com.tangem.data.wallets

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.data.wallets.converters.UserWalletRemoteInfoConverter
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException
import com.tangem.datasource.api.common.response.fold
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.*
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMap
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.UserWalletRemoteInfo
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
internal class DefaultWalletsRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val tangemTechApi: TangemTechApi,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val walletServerBinder: WalletServerBinder,
    private val moshi: com.squareup.moshi.Moshi,
) : WalletsRepository {

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

    override suspend fun createWallet(userWalletId: UserWalletId) {
        walletServerBinder.bind(userWalletId)
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
        val userWallet = userWalletsListRepository.getSyncOrNull(id = walletId)

        tangemTechApi.updateWallet(
            walletId = walletId.stringValue,
            body = WalletBody(name = walletName, type = WalletType.from(userWallet)),
        ).getOrThrow()
    }

    override suspend fun upgradeWallet(walletId: UserWalletId) = withContext(dispatchers.io) {
        val userWallet = userWalletsListRepository.getSyncStrict(id = walletId)

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
            val associateApplicationIdWithWallets: suspend () -> ApiResponse<Unit> = {
                tangemTechApi.associateApplicationIdWithWallets(
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
        }

    override suspend fun activatePromoCode(
        userWalletId: UserWalletId,
        promoCode: String,
        bitcoinAddress: String,
    ): Either<ActivatePromoCodeError, String> = withContext(dispatchers.io) {
        tangemTechApi.activatePromoCode(
            body = PromocodeActivationBody(
                promoCode = promoCode,
                address = bitcoinAddress,
                walletId = userWalletId.stringValue,
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