package com.tangem.tap.domain.userWalletList.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.CompletionResult
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.flatMap
import com.tangem.common.map
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.core.wallets.UserWalletsListRepository.LockMethod
import com.tangem.domain.core.wallets.error.*
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.R
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.utils.encryptionKey
import com.tangem.tap.domain.userWalletList.utils.lock
import com.tangem.tap.domain.userWalletList.utils.toUserWallets
import com.tangem.tap.domain.userWalletList.utils.updateWith
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend
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

        // update the userWallets state and add if it doesn't exist
        updateWallets { currentWallets ->
            val wallets = currentWallets ?: emptyList()
            if (wallets.any { it.walletId == userWallet.walletId }) {
                wallets.map { if (it.walletId == userWallet.walletId) userWallet else it }
            } else {
                wallets + userWallet
            }
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

        runCatching {
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
                select(userWalletId)
            }
            UserWalletsListRepository.UnlockMethod.AccessCode -> {
                if (userWallet !is UserWallet.Hot) {
                    raise(UnlockWalletError.UnableToUnlock)
                }

                val encryptionKey = requestPasswordRecursive(
                    hotWalletId = userWallet.hotWalletId,
                    block = { password ->
                        runCatching {
                            userWalletEncryptionKeysRepository.getEncryptedWithPassword(userWalletId, password)
                        }.onFailure {
                            raise(UnlockWalletError.UnableToUnlock)
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
                    .doOnSuccess { sensitiveInfo -> updateWallets { it?.updateWith(sensitiveInfo) } }
                    .doOnFailure { error ->
                        raise(UnlockWalletError.UnableToUnlock)
                    }
            }
            is UserWalletsListRepository.UnlockMethod.Scan -> {
                if (userWallet !is UserWallet.Cold) {
                    raise(UnlockWalletError.UnableToUnlock)
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
                    encryptionKey = scanResponse.encryptionKey ?: raise(UnlockWalletError.UnableToUnlock),
                )

                sensitiveInformationRepository.getAll(listOf(encryptionKey))
                    .doOnSuccess { sensitiveInfo -> updateWallets { it?.updateWith(sensitiveInfo) } }
                    .doOnFailure { error -> raise(UnlockWalletError.UnableToUnlock) }
            }
        }
    }

    override suspend fun unlockAllWallets(): Either<UnlockWalletError, Unit> = either {
        val userWallets = userWalletsSync()
        val userWalletIds = userWallets.map { it.walletId }.toSet()
        val biometricKeys = runCatching {
            userWalletEncryptionKeysRepository.getAllBiometric()
        }.getOrElse {
            // TODO handle error properly [REDACTED_TASK_KEY]
            raise(UnlockWalletError.UserCancelled)
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

        // if we cant unlock all wallets
        if (userWalletIds.all { it in unlockedWalletsIds }.not()) {
            raise(UnlockWalletError.UnableToUnlock)
        }

        sensitiveInformationRepository.getAll(allKeys)
            .doOnSuccess { sensitiveInfo ->
                updateWallets { userWallets.updateWith(sensitiveInfo) }
            }
            .doOnFailure { raise(UnlockWalletError.UnableToUnlock) }
    }

    override suspend fun lockAllWallets(): Either<LockWalletsError, Unit> = either {
        val unsecuredWalletIds = userWalletEncryptionKeysRepository.getAllUnsecured().map { it.walletId }.toSet()

        if (unsecuredWalletIds.size == userWallets.value?.size) {
            raise(LockWalletsError.NothingToLock)
        }

        updateWallets {
            it?.map {
                if (it.walletId !in unsecuredWalletIds) {
                    it.lock()
                } else {
                    it
                }
            }
        }
    }

    override suspend fun clearPersistentData() {
        publicInformationRepository.clear()
        sensitiveInformationRepository.clear()
        userWalletEncryptionKeysRepository.clear()
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
        val useBiometricAuthentication = appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.USE_BIOMETRIC_AUTHENTICATION_KEY,
            default = false,
        )

        return tangemSdkManagerProvider.invoke().canUseBiometry && useBiometricAuthentication
    }

    private fun updateWallets(block: (List<UserWallet>?) -> List<UserWallet>?) {
        userWallets.update {
            val updated = block(it)
            selectedUserWallet.update { currentSelected ->
                if (currentSelected == null) return@update null
                updated?.find { it.walletId == currentSelected.walletId }
            }
            updated
        }
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
}