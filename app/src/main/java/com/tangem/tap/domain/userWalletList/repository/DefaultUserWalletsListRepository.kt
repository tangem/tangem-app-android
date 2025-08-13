package com.tangem.tap.domain.userWalletList.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.flatMap
import com.tangem.common.map
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.R
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.domain.core.wallets.error.DeleteWalletError
import com.tangem.domain.core.wallets.error.LockWalletsError
import com.tangem.domain.core.wallets.error.SaveWalletError
import com.tangem.domain.core.wallets.error.SelectWalletError
import com.tangem.domain.core.wallets.error.SetLockError
import com.tangem.domain.core.wallets.error.UnlockWalletError
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.core.wallets.UserWalletsListRepository.LockMethod
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.utils.encryptionKey
import com.tangem.tap.domain.userWalletList.utils.lock
import com.tangem.tap.domain.userWalletList.utils.toUserWallets
import com.tangem.tap.domain.userWalletList.utils.updateWith
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Suppress("LongParameterList")
internal class DefaultUserWalletsListRepository(
    private val publicInformationRepository: UserWalletsPublicInformationRepository,
    private val sensitiveInformationRepository: UserWalletsSensitiveInformationRepository,
    private val selectedUserWalletRepository: SelectedUserWalletRepository,
    private val passwordRequester: HotWalletPasswordRequester,
    private val userWalletEncryptionKeysRepository: UserWalletEncryptionKeysRepository,
    private val tangemSdkManagerProvider: Provider<TangemSdkManager>,
    private val savePersistentInformation: ProviderSuspend<Boolean>,
) : UserWalletsListRepository {

    override val userWallets = MutableStateFlow<List<UserWallet>?>(null)
    override val selectedUserWallet = MutableStateFlow<UserWallet?>(null)

    override suspend fun load() {
        if (userWallets.value != null) return

        if (savePersistentInformation().not()) {
            // If we don't save persistent information, we don't need to load user wallets
            // and we should clear any existing data
            clearPersistentData()
            userWallets.value = emptyList()
            return
        }

        val unsecuredEncryptionKeys = userWalletEncryptionKeysRepository.getAllUnsecured()

        publicInformationRepository.getAll()
            .map { it.toUserWallets() }
            .flatMap { wallets ->
                sensitiveInformationRepository.getAll(unsecuredEncryptionKeys)
                    .map { wallets.updateWith(it) }
            }.doOnSuccess {
                userWallets.value = it
            }

        val selectedUserWalletId = selectedUserWalletRepository.get()
        selectedUserWallet.value = userWallets.value?.firstOrNull { it.walletId == selectedUserWalletId }
            ?: userWallets.value?.firstOrNull()
    }

    override suspend fun userWalletsSync(): List<UserWallet> {
        load()
        return userWallets.value!!
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
        userWallets.update { currentWallets ->
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

    override suspend fun setLock(userWalletId: UserWalletId, lockMethod: LockMethod): Either<SetLockError, Unit> =
        either {
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
            currentWallets?.filterNot { it.walletId in userWalletIds }
        }

        selectedUserWallet.update { currentSelected ->
            if (currentSelected == null) return@update null

            userWallets.value?.findAvailableUserWallet(
                userWallets.value?.indexOfFirst { it.walletId == currentSelected.walletId } ?: 0,
            )
        }
    }

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
                unlockAllWallets()
                select(userWalletId)
            }
            UserWalletsListRepository.UnlockMethod.AccessCode -> {
                if (userWallet !is UserWallet.Hot) {
                    raise(UnlockWalletError.UnableToUnlock)
                }

                val encryptionKey = requestPasswordRecursive(
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

                sensitiveInformationRepository.getAll(listOf(encryptionKey))
                    .doOnSuccess { userWallets.value?.updateWith(it) }
                    .doOnFailure { error ->
                        raise(UnlockWalletError.UnableToUnlock)
                    }
            }
            UserWalletsListRepository.UnlockMethod.Scan -> {
                if (userWallet !is UserWallet.Cold) {
                    raise(UnlockWalletError.UnableToUnlock)
                }

                tangemSdkManagerProvider().scanProduct()
                    .doOnSuccess { scanResponse ->
                        val expectedId = UserWalletIdBuilder.scanResponse(scanResponse).build()

                        if (expectedId != userWallet.walletId) {
                            raise(UnlockWalletError.ScannedCardWalletNotMatched)
                        }

                        saveWithoutLock(userWallet.copy(scanResponse = scanResponse), canOverride = true)
                            .mapLeft { UnlockWalletError.UnableToUnlock }
                            .bind()
                    }
                    .doOnFailure {
                        raise(UnlockWalletError.UserCancelled)
                    }
            }
        }
    }

    override suspend fun unlockAllWallets(): Either<UnlockWalletError, Unit> = either {
        val biometricKeys = runCatching {
            userWalletEncryptionKeysRepository.getAllBiometric()
        }.getOrElse {
            // TODO handle error properly [REDACTED_TASK_KEY]
            raise(UnlockWalletError.UserCancelled)
        }

        val unsecuredKeys = userWalletEncryptionKeysRepository.getAllUnsecured()
        val allKeys = biometricKeys + unsecuredKeys
        sensitiveInformationRepository.getAll(allKeys)
            .doOnSuccess { userWallets.value?.updateWith(it) }
    }

    override suspend fun lockAllWallets(): Either<LockWalletsError, Unit> = either {
        val unsecuredWalletIds = userWalletEncryptionKeysRepository.getAllUnsecured().map { it.walletId }.toSet()

        if (unsecuredWalletIds.size == userWallets.value?.size) {
            raise(LockWalletsError.NothingToLock)
        }

        userWallets.update {
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
        block: suspend (CharArray) -> UserWalletEncryptionKey?,
        biometryFallback: suspend () -> Either<UnlockWalletError, Unit>,
    ): Either<UnlockWalletError, UserWalletEncryptionKey?> {
        val result = passwordRequester.requestPassword(
            hasBiometry = tangemSdkManagerProvider.invoke().needEnrollBiometrics,
        )

        return when (result) {
            HotWalletPasswordRequester.Result.Dismiss -> {
                passwordRequester.dismiss()
                UnlockWalletError.UserCancelled.left()
            }
            is HotWalletPasswordRequester.Result.EnteredPassword -> {
                val decrypted = block(result.password.value)
                if (decrypted == null) {
                    passwordRequester.wrongPassword()
                    requestPasswordRecursive(block, biometryFallback)
                } else {
                    passwordRequester.successfulAuthentication()
                    decrypted.right()
                }
            }
            HotWalletPasswordRequester.Result.UseBiometry -> {
                biometryFallback()
                    .onRight {
                        passwordRequester.successfulAuthentication()
                    }
                passwordRequester.dismiss()
                null.right()
            }
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