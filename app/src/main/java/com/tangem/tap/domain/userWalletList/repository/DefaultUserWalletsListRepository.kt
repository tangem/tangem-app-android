package com.tangem.tap.domain.userWalletList.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.*
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository.LockMethod
import com.tangem.domain.common.wallets.error.*
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.R
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.utils.encryptionKey
import com.tangem.tap.domain.userWalletList.utils.lock
import com.tangem.tap.domain.userWalletList.utils.toUserWallets
import com.tangem.tap.domain.userWalletList.utils.updateWith
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.extensions.addOrReplace
import com.tangem.utils.extensions.indexOfFirstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Suppress("LongParameterList", "LargeClass")
internal class DefaultUserWalletsListRepository(
    private val publicInformationRepository: UserWalletsPublicInformationRepository,
    private val sensitiveInformationRepository: UserWalletsSensitiveInformationRepository,
    private val selectedUserWalletRepository: SelectedUserWalletRepository,
    private val passwordRequester: HotWalletPasswordRequester,
    private val userWalletEncryptionKeysRepository: UserWalletEncryptionKeysRepository,
    private val tangemSdkManagerProvider: Provider<TangemSdkManager>,
    private val savePersistentInformation: ProviderSuspend<Boolean>,
    private val appPreferencesStore: AppPreferencesStore,
    private val hotWalletAccessCodeAttemptsRepository: HotWalletAccessCodeAttemptsRepository,
    private val tangemHotSdk: TangemHotSdk,
    private val trackingContextProxy: TrackingContextProxy,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val hotWalletRepository: HotWalletRepository,
) : UserWalletsListRepository {

    override val userWallets = MutableStateFlow<List<UserWallet>?>(null)
    override val selectedUserWallet = MutableStateFlow<UserWallet?>(null)
    private val mutex = Mutex()

    override suspend fun load() {
        mutex.withLock {
            if (userWallets.value != null) return

            if (savePersistentInformation().not()) {
                // If we don't save persistent information, we don't need to load user wallets
                // and we should clear any existing data
                clearPersistentData()
                updateWallets { emptyList() }
                return
            }

            val unsecuredEncryptionKeys = userWalletEncryptionKeysRepository.getAllUnsecured()

            publicInformationRepository.getAll()
                .map { it.toUserWallets() }
                .flatMap { wallets ->
                    sensitiveInformationRepository.getAll(unsecuredEncryptionKeys)
                        .map { wallets.updateWith(it) }
                }
                .doOnSuccess { loadedWallets ->
                    userWallets.update { toUpdate ->
                        val selectedUserWalletId = selectedUserWalletRepository.get()
                        selectedUserWallet.value = loadedWallets.firstOrNull { it.walletId == selectedUserWalletId }
                            ?: loadedWallets.firstOrNull()?.also {
                                selectedUserWalletRepository.set(it.walletId)
                            }

                        loadedWallets
                    }
                }
        }
    }

    override suspend fun userWalletsSync(): List<UserWallet> {
        load()
        return requireNotNull(userWallets.value) {
            "This should never happen"
        }
    }

    override suspend fun selectedUserWalletSync(): UserWallet? {
        load()
        return selectedUserWallet.value
    }

    override suspend fun select(userWalletId: UserWalletId): Either<SelectWalletError, UserWallet> = either {
        val userWallet = userWallets.value?.find { it.walletId == userWalletId }
            ?: raise(SelectWalletError.UnableToSelectUserWallet)
        selectedUserWalletRepository.set(userWalletId)
        selectedUserWallet.value = userWallet
        userWallet
    }

    override suspend fun saveWithoutLock(
        userWallet: UserWallet,
        canOverride: Boolean,
    ): Either<SaveWalletError, UserWallet> = either {
        if (canOverride.not() && userWallets.value?.any { it.walletId == userWallet.walletId } == true) {
            raise(SaveWalletError.WalletAlreadySaved(messageId = R.string.user_wallet_list_error_wallet_already_saved))
        }

        if (savePersistentInformation()) {
            publicInformationRepository.save(userWallet, canOverride)
            if (userWallet.isLocked.not()) {
                sensitiveInformationRepository.save(userWallet, userWallet.encryptionKey)
            }
        }

        val oldUserWallet = userWalletsSync().find { it.walletId == userWallet.walletId }

        if (oldUserWallet != null) {
            checkForUpgradeAndDeleteHotWalletIfNeeded(
                newUserWallet = userWallet,
                oldUserWallet = oldUserWallet,
            )
        }

        // update the userWallets state and add if it doesn't exist
        updateWallets { currentWallets ->
            val wallets = currentWallets.orEmpty()
            wallets.addOrReplace(userWallet) { it.walletId == userWallet.walletId }
        }

        // update the selectedUserWallet state if it is the only wallet
        if (userWallets.value?.size == 1) {
            selectedUserWalletRepository.set(userWallet.walletId)
            selectedUserWallet.value = userWallet
        }

        userWallet
    }

    override suspend fun setLock(
        userWalletId: UserWalletId,
        lockMethod: LockMethod,
        changeUnsecured: Boolean,
    ): Either<SetLockError, Unit> = either {
        val userWallet = userWallets.value?.find { it.walletId == userWalletId }
            ?: raise(SetLockError.UserWalletNotFound)

        val encryptionKey = userWallet.encryptionKey
            ?: raise(SetLockError.UserWalletLocked)

        runSuspendCatching {
            userWalletEncryptionKeysRepository.save(
                encryptionKey = UserWalletEncryptionKey(
                    walletId = userWalletId,
                    encryptionKey = encryptionKey,
                ),
                removeUnsecured = changeUnsecured,
                method = when (lockMethod) {
                    is LockMethod.AccessCode -> {
                        UserWalletEncryptionKeysRepository.EncryptionMethod.Password(lockMethod.accessCode)
                    }
                    LockMethod.Biometric -> {
                        UserWalletEncryptionKeysRepository.EncryptionMethod.Biometric
                    }
                    LockMethod.NoLock -> {
                        if (userWallet is UserWallet.Cold) {
                            raise(SetLockError.UserWalletNotFound)
                        }

                        UserWalletEncryptionKeysRepository.EncryptionMethod.Unsecured
                    }
                },
            )
        }.onFailure { raise(SetLockError.UnableToSetLock(it)) }
    }

    override suspend fun removeBiometricLock(userWalletId: UserWalletId) {
        userWalletEncryptionKeysRepository.removeBiometricKey(userWalletId)
    }

    override suspend fun delete(userWalletIds: List<UserWalletId>): Either<DeleteWalletError, Unit> = either {
        if (userWalletIds.isEmpty()) return Unit.right()

        publicInformationRepository.delete(userWalletIds)
            .doOnFailure {
                raise(DeleteWalletError.UnableToDelete)
            }
        sensitiveInformationRepository.delete(userWalletIds)
            .doOnFailure {
                raise(DeleteWalletError.UnableToDelete)
            }

        userWalletEncryptionKeysRepository.delete(userWalletIds)

        removeHotWalletsFromSDKAndRepos(userWalletIds)

        userWallets.update { currentWallets ->
            val updatedWallets = currentWallets?.filter { userWalletIds.contains(it.walletId).not() }
            selectedUserWallet.update { currentSelected ->
                if (currentSelected == null) return@update null
                val newSelected = updatedWallets?.findAvailableUserWallet(
                    currentWallets.indexOfFirstOrNull { it.walletId == currentSelected.walletId } ?: 0,
                )
                selectedUserWalletRepository.set(newSelected?.walletId)
                newSelected
            }
            updatedWallets
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override suspend fun unlock(
        userWalletId: UserWalletId,
        unlockMethod: UserWalletsListRepository.UnlockMethod,
    ): Either<UnlockWalletError, Unit> = either {
        val userWallet = userWallets.value?.find { it.walletId == userWalletId }
            ?: raise(UnlockWalletError.UserWalletNotFound)

        if (userWallet.isLocked.not()) {
            raise(UnlockWalletError.AlreadyUnlocked)
        }

        when (unlockMethod) {
            UserWalletsListRepository.UnlockMethod.Biometric -> {
                unlockAllWallets().bind()
                trackSignInEvent(userWallet, Basic.SignedIn.SignInType.Biometric)
                select(userWalletId)
            }
            UserWalletsListRepository.UnlockMethod.AccessCode -> {
                if (userWallet !is UserWallet.Hot) {
                    raise(UnlockWalletError.UnableToUnlock.Empty)
                }

                val encryptionKey = requestPasswordRecursive(
                    hotWalletId = userWallet.hotWalletId,
                    block = { password ->
                        runSuspendCatching {
                            userWalletEncryptionKeysRepository.getEncryptedWithPassword(userWalletId, password)
                        }.onFailure { error ->
                            raise(UnlockWalletError.UnableToUnlock.RawException(error))
                        }.getOrNull()
                    },
                    biometryFallback = {
                        unlock(userWalletId, UserWalletsListRepository.UnlockMethod.Biometric)
                    },
                ).bind()

                if (encryptionKey == null) {
                    return@either
                }

                removePasswordAttempts(userWallet)

                sensitiveInformationRepository.getAll(listOf(encryptionKey))
                    .doOnSuccess { sensitiveInfo ->
                        updateWallets { it?.updateWith(sensitiveInfo) }
                        trackSignInEvent(userWallet, Basic.SignedIn.SignInType.AccessCode)
                    }
                    .doOnFailure { error -> raise(UnlockWalletError.UnableToUnlock.RawException(error)) }
            }
            is UserWalletsListRepository.UnlockMethod.Scan -> {
                if (userWallet !is UserWallet.Cold) {
                    raise(UnlockWalletError.UnableToUnlock.Empty)
                }

                val scanResponse = unlockMethod.scanResponse ?: run {
                    val res = tangemSdkManagerProvider().scanProduct()
                    when (res) {
                        is CompletionResult.Failure -> raise(UnlockWalletError.UserCancelled)
                        is CompletionResult.Success -> res.data
                    }
                }

                val expectedId = UserWalletIdBuilder.scanResponse(scanResponse).build()

                if (expectedId != userWallet.walletId) {
                    raise(UnlockWalletError.ScannedCardWalletNotMatched)
                }

                val encryptionKey = UserWalletEncryptionKey(
                    walletId = userWallet.walletId,
                    encryptionKey = scanResponse.encryptionKey ?: raise(UnlockWalletError.UnableToUnlock.Empty),
                )

                sensitiveInformationRepository.getAll(listOf(encryptionKey))
                    .doOnSuccess { sensitiveInfo ->
                        updateWallets { it?.updateWith(sensitiveInfo) }
                        trackSignInEvent(userWallet, Basic.SignedIn.SignInType.Card)
                    }
                    .doOnFailure { error -> raise(UnlockWalletError.UnableToUnlock.RawException(error)) }
            }
        }
    }

    override suspend fun unlockAllWallets(): Either<UnlockWalletError, Unit> = either {
        val userWallets = userWalletsSync().filter { it.isLocked }

        if (userWallets.isEmpty()) {
            return@either
        }

        val biometricKeys = runSuspendCatching {
            userWalletEncryptionKeysRepository.getAllBiometric()
        }.getOrElse { exception ->
            catchBiometricException(exception)
        }

        val unsecuredKeys = userWalletEncryptionKeysRepository.getAllUnsecured()
        val allKeys = (biometricKeys + unsecuredKeys).distinct()
        val unlockedWalletsIds = allKeys.map { it.walletId }

        val unlockedWallets = unlockedWalletsIds.mapNotNull { id ->
            userWallets.firstOrNull { it.walletId == id }
        }

        // Remove all password attempts for unlocked hot wallets
        unlockedWallets.forEach {
            removePasswordAttempts(it)
        }

        // if we cant unlock any of the locked wallets, return error
        // (isLocked remains `true` here because we haven't updated the wallets yet)
        if (unlockedWallets.any { it.isLocked }.not()) {
            raise(UnlockWalletError.UnableToUnlock.Empty)
        }

        sensitiveInformationRepository.getAll(allKeys)
            .doOnSuccess { sensitiveInfo ->
                updateWallets { wallets -> wallets?.updateWith(sensitiveInfo) }
                selectedUserWallet.value?.let {
                    trackSignInEvent(it, Basic.SignedIn.SignInType.Biometric)
                }
            }
            .doOnFailure { error -> raise(UnlockWalletError.UnableToUnlock.RawException(error)) }
    }

    override suspend fun lockAllWallets(): Either<LockWalletsError, Unit> = either {
        val unsecuredWalletIds = userWalletEncryptionKeysRepository.getAllUnsecured().map { it.walletId }.toSet()

        if (unsecuredWalletIds.size == userWallets.value?.size) {
            raise(LockWalletsError.NothingToLock)
        }

        updateWallets { wallets ->
            wallets?.map { wallet ->
                if (wallet.walletId !in unsecuredWalletIds) {
                    wallet.lock()
                } else {
                    wallet
                }
            }
        }
    }

    override suspend fun clearPersistentData() {
        publicInformationRepository.clear()
        sensitiveInformationRepository.clear()
        userWalletEncryptionKeysRepository.clear()
    }

    override suspend fun hasSecuredWallets(): Boolean {
        val userWallets = userWalletsSync()
        val unsecuredWalletIds = userWalletEncryptionKeysRepository.getAllUnsecured().map { it.walletId }.toSet()
        return userWallets.any { it.walletId !in unsecuredWalletIds }
    }

    private suspend fun checkForUpgradeAndDeleteHotWalletIfNeeded(
        newUserWallet: UserWallet,
        oldUserWallet: UserWallet,
    ) {
        if (newUserWallet.walletId == oldUserWallet.walletId &&
            oldUserWallet is UserWallet.Hot && newUserWallet is UserWallet.Cold
        ) {
            trackWalletUpgradeEvent()
            removeHotWalletsFromSDKAndRepos(walletIds = listOf(oldUserWallet.walletId))
            // When upgrading from Hot to Cold, if biometric lock is available, set it
            if (hasBiometry()) {
                setLock(newUserWallet.walletId, LockMethod.Biometric, changeUnsecured = true)
            }
            // Remove any encryption keys related to the old hot wallet
            userWalletEncryptionKeysRepository.removeEncryptedWithPasswordKey(oldUserWallet.walletId)
            userWalletEncryptionKeysRepository.removeUnsecuredKey(oldUserWallet.walletId)
        }
    }

    private suspend fun removeHotWalletsFromSDKAndRepos(walletIds: List<UserWalletId>) {
        val hotWalletsToDelete = userWalletsSync()
            .filterIsInstance<UserWallet.Hot>()
            .filter { walletIds.contains(it.walletId) }

        hotWalletsToDelete.forEach { wallet ->
            hotWalletRepository.setAccessCodeSkipped(wallet.walletId, false) // In case the wallet is added again
            tangemHotSdk.delete(wallet.hotWalletId)
        }
    }

    private suspend fun requestPasswordRecursive(
        hotWalletId: HotWalletId,
        block: suspend (CharArray) -> UserWalletEncryptionKey?,
        biometryFallback: suspend () -> Either<UnlockWalletError, Unit>,
    ): Either<UnlockWalletError, UserWalletEncryptionKey?> {
        val attemptRequest = HotWalletPasswordRequester.AttemptRequest(
            hotWalletId = hotWalletId,
            authMode = true, // In auth mode user wallet can be deleted after 30 failed attempts
            hasBiometry = hasBiometry(),
        )
        val result = passwordRequester.requestPassword(attemptRequest)

        return when (result) {
            HotWalletPasswordRequester.Result.Dismiss -> {
                passwordRequester.dismiss()
                UnlockWalletError.UserCancelled.left()
            }
            is HotWalletPasswordRequester.Result.EnteredPassword -> {
                val decrypted = block(result.password.value)
                if (decrypted == null) {
                    passwordRequester.wrongPassword()
                    requestPasswordRecursive(hotWalletId, block, biometryFallback)
                } else {
                    passwordRequester.successfulAuthentication()
                    passwordRequester.dismiss()
                    decrypted.right()
                }
            }
            HotWalletPasswordRequester.Result.UseBiometry -> {
                biometryFallback()
                    .onRight {
                        passwordRequester.successfulAuthentication()
                        passwordRequester.dismiss()
                    }
                    .map { null }
            }
        }
    }

    private suspend fun removePasswordAttempts(userWallet: UserWallet) {
        if (userWallet is UserWallet.Hot) {
            hotWalletAccessCodeAttemptsRepository.resetAttempts(userWallet.hotWalletId)
        }
    }

    private suspend fun hasBiometry(): Boolean {
        val isBiometricAuthenticationUsed = appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.USE_BIOMETRIC_AUTHENTICATION_KEY,
            default = false,
        )

        return tangemSdkManagerProvider.invoke().canUseBiometry && isBiometricAuthenticationUsed
    }

    private fun updateWallets(block: (List<UserWallet>?) -> List<UserWallet>?) {
        userWallets.update { wallets ->
            val updated = block(wallets)
            selectedUserWallet.update { currentSelected ->
                if (currentSelected == null) return@update null
                updated?.find { it.walletId == currentSelected.walletId }
            }
            updated
        }
    }

    private fun Raise<UnlockWalletError>.catchBiometricException(ex: Throwable): Nothing {
        val reason = when (ex) {
            is TangemSdkError.AuthenticationLockout ->
                UnlockWalletError.UnableToUnlock.Reason.BiometricsAuthenticationLockout(isPermanent = false)
            is TangemSdkError.AuthenticationPermanentLockout ->
                UnlockWalletError.UnableToUnlock.Reason.BiometricsAuthenticationLockout(isPermanent = true)
            is TangemSdkError.KeystoreInvalidated ->
                UnlockWalletError.UnableToUnlock.Reason.AllKeysInvalidated
            is TangemSdkError.AuthenticationUnavailable ->
                UnlockWalletError.UnableToUnlock.Reason.BiometricsAuthenticationDisabled

            is TangemSdkError.AuthenticationCanceled,
            is TangemSdkError.AuthenticationAlreadyInProgress,
            -> raise(UnlockWalletError.UserCancelled)

            else -> raise(UnlockWalletError.UnableToUnlock.RawException(ex))
        }

        raise(UnlockWalletError.UnableToUnlock.WithReason(reason))
    }

    /**
     * Find the nearest available wallet that can be selected
     *
     * Example:
     * Number with *n* is previous selected wallet with index [prevSelectedIndex].
     *
     * 1. [*1*, 2, 3, 4] => delete 1 => [2, 3, 4] => find and select => [*2*, 3, 4]
     * 2. [1, *2*, 3, 4] => delete 2 => [1, 3, 4] => find and select => [1, *3*, 4]
     * 3. [1, 2, *3*, 4] => delete 3 => [1, 2, 4] => find and select => [1, 2, *4*]
     * 4. [1, 2, 3, *4*] => delete 4 => [1, 2, 3] => find and select => [1, 2, *3*]
     *
     * @receiver list of user wallets without deleted wallet
     */
    private fun List<UserWallet>.findAvailableUserWallet(prevSelectedIndex: Int): UserWallet? {
        if (prevSelectedIndex == 0) return firstOrNull { !it.isLocked } ?: firstOrNull()

        if (prevSelectedIndex in indices && !this[prevSelectedIndex].isLocked) return this[prevSelectedIndex]

        for (offset in 1..size) {
            val rightIndex = prevSelectedIndex + offset
            if (rightIndex in indices && !this[rightIndex].isLocked) return this[rightIndex]

            val leftIndex = prevSelectedIndex - offset
            if (leftIndex in indices && !this[leftIndex].isLocked) return this[leftIndex]
        }

        return lastOrNull()
    }

    private fun trackSignInEvent(userWallet: UserWallet, type: Basic.SignedIn.SignInType) {
        trackingContextProxy.addContext(userWallet)
        analyticsEventHandler.send(
            event = Basic.SignedIn(
                signInType = type,
                walletsCount = userWallets.value?.size ?: 0,
            ),
        )
    }

    private fun trackWalletUpgradeEvent() {
        analyticsEventHandler.send(event = WalletSettingsAnalyticEvents.WalletUpgraded())
    }
}