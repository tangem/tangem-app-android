package com.tangem.tap.domain.userWalletList.implementation

import com.tangem.common.*
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.UserWalletsListManager.Lockable.UnlockType
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.models.isLocked
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.repository.SelectedUserWalletRepository
import com.tangem.tap.domain.userWalletList.repository.UserWalletsKeysRepository
import com.tangem.tap.domain.userWalletList.repository.UserWalletsPublicInformationRepository
import com.tangem.tap.domain.userWalletList.repository.UserWalletsSensitiveInformationRepository
import com.tangem.tap.domain.userWalletList.utils.encryptionKey
import com.tangem.tap.domain.userWalletList.utils.lockAll
import com.tangem.tap.domain.userWalletList.utils.toUserWallets
import com.tangem.tap.domain.userWalletList.utils.updateWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
internal class BiometricUserWalletsListManager(
    private val keysRepository: UserWalletsKeysRepository,
    private val publicInformationRepository: UserWalletsPublicInformationRepository,
    private val sensitiveInformationRepository: UserWalletsSensitiveInformationRepository,
    private val selectedUserWalletRepository: SelectedUserWalletRepository,
) : UserWalletsListManager.Lockable {
    private val state = MutableStateFlow(State())

    override val isLockable: Boolean = true

    override val userWallets: Flow<List<UserWallet>>
        get() = state
            .mapLatest { it.userWallets }
            .distinctUntilChanged()

    override val savedWalletsCount: Flow<Int>
        get() = state
            .mapLatest { walletsCount }
            .distinctUntilChanged()

    override val userWalletsSync: List<UserWallet>
        get() = state.value.userWallets

    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    override val selectedUserWallet: Flow<UserWallet>
        get() = state
            .mapLatest { state ->
                findSelectedUserWallet(state.userWallets)
            }
            .filterNotNull()
            .distinctUntilChanged()

    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    override val selectedUserWalletSync: UserWallet?
        get() = findSelectedUserWallet()

    override val isLocked: Flow<Boolean>
        get() = state
            .mapLatest { it.isLocked }
            .distinctUntilChanged()

    override val isLockedSync: Boolean
        get() = state.value.isLocked

    override val hasUserWallets: Boolean
        get() = keysRepository.hasSavedEncryptionKeys()

    override val walletsCount: Int
        get() = state.value.userWallets.size

    override suspend fun unlock(type: UnlockType): CompletionResult<UserWallet> {
        return unlockAndSetSelectedUserWallet(type)
            .mapFailure { error ->
                Timber.e(error, "Unable to unlock user wallets")
                if (error is UserWalletsListError) {
                    error
                } else {
                    UserWalletsListError.UnableToUnlockUserWallets(error)
                }
            }
            .map { selectedUserWallet ->
                if (selectedUserWallet == null || selectedUserWallet.isLocked) {
                    Timber.e("Unable to find selected user wallet")
                    throw UserWalletsListError.NoUserWalletSelected
                } else {
                    selectedUserWallet
                }
            }
    }

    override fun lock() {
        state.update { prevState ->
            prevState.copy(
                encryptionKeys = emptyList(),
                userWallets = prevState.userWallets.lockAll(),
                isLocked = true,
            )
        }
    }

    override suspend fun select(userWalletId: UserWalletId): CompletionResult<UserWallet> = catching {
        if (state.value.selectedUserWalletId == userWalletId) {
            return@catching findSelectedUserWallet()!!
        }

        selectedUserWalletRepository.set(userWalletId)

        val newState = state.updateAndGet { prevState ->
            prevState.copy(
                selectedUserWalletId = userWalletId,
            )
        }

        newState.userWallets.first { it.walletId == userWalletId }
    }

    override suspend fun save(userWallet: UserWallet, canOverride: Boolean): CompletionResult<Unit> {
        return if (canOverride) {
            saveInternal(userWallet, changeSelectedUserWallet = true, canOverridePublicInfo = false)
        } else {
            val isWalletSaved = state.value.userWallets
                .any {
                    it.walletId == userWallet.walletId
                }

            if (isWalletSaved) {
                CompletionResult.Failure(UserWalletsListError.WalletAlreadySaved)
            } else {
                saveInternal(userWallet, changeSelectedUserWallet = true, canOverridePublicInfo = false)
            }
        }
    }

    override suspend fun update(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): CompletionResult<UserWallet> {
        return get(userWalletId)
            .map { storedUserWallet ->
                update(storedUserWallet)
            }
            .flatMap { updatedUserWallet ->
                saveInternal(updatedUserWallet, changeSelectedUserWallet = false, canOverridePublicInfo = true)
            }
            .flatMap {
                get(userWalletId)
            }
    }

    override suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit> {
        val idsToRemove = state.value.userWallets
            .takeIf { it.isNotEmpty() }
            ?.filter { it.walletId in userWalletIds }
            ?.map { it.walletId }

        if (idsToRemove.isNullOrEmpty()) {
            return CompletionResult.Success(Unit)
        }

        if (idsToRemove.size == state.value.userWallets.size) {
            return clear()
        }

        return sensitiveInformationRepository.delete(idsToRemove)
            .flatMap { publicInformationRepository.delete(idsToRemove) }
            .map { keysRepository.delete(idsToRemove) }
            .map {
                state.update { prevState ->
                    val remainingWallets = prevState.userWallets.filter { it.walletId !in idsToRemove }

                    val isSelectedWalletDeleted = prevState.selectedUserWalletId in idsToRemove
                    val newSelectedUserWallet = findOrSetSelectedWallet(
                        prevSelectedWalletId = prevState.selectedUserWalletId,
                        prevSelectedWalletIndex = prevState.userWallets.indexOfFirst {
                            it.walletId == prevState.selectedUserWalletId
                        },
                        userWallets = remainingWallets,
                        ignorePrevSelectedWallet = isSelectedWalletDeleted,
                    )

                    prevState.copy(
                        encryptionKeys = prevState.encryptionKeys.filter { it.walletId !in idsToRemove },
                        userWallets = remainingWallets,
                        isLocked = remainingWallets.any { it.isLocked },
                        selectedUserWalletId = newSelectedUserWallet?.walletId,
                    )
                }
            }
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return sensitiveInformationRepository.clear()
            .flatMap { publicInformationRepository.clear() }
            .map {
                keysRepository.clear()
                selectedUserWalletRepository.set(null)
                state.value = State()
            }
    }

    override suspend fun get(userWalletId: UserWalletId): CompletionResult<UserWallet> {
        return catching {
            state.value.userWallets.first { it.walletId == userWalletId }
        }
    }

    private suspend fun saveInternal(
        userWallet: UserWallet,
        changeSelectedUserWallet: Boolean,
        canOverridePublicInfo: Boolean,
    ): CompletionResult<Unit> {
        val encryptionKey = userWallet.encryptionKey
            ?.let { UserWalletEncryptionKey(userWallet.walletId, it) }
            ?: return CompletionResult.Success(Unit) // No encryption key, no need to save

        return keysRepository.save(encryptionKey)
            .flatMap { sensitiveInformationRepository.save(userWallet, encryptionKey = encryptionKey.encryptionKey) }
            .flatMap { publicInformationRepository.save(userWallet, canOverridePublicInfo) }
            .flatMap {
                loadUserWallets(
                    encryptionKeys = state.value.encryptionKeys
                        .plus(encryptionKey)
                        .distinctBy(UserWalletEncryptionKey::walletId),
                )
            }
            .doOnSuccess { loadedState ->
                if (changeSelectedUserWallet) {
                    selectedUserWalletRepository.set(userWallet.walletId)

                    state.value = loadedState.copy(
                        selectedUserWalletId = userWallet.walletId,
                    )
                } else {
                    state.value = loadedState
                }
            }
            .map { /* Type erasing */ }
    }

    private suspend fun unlockAndSetSelectedUserWallet(type: UnlockType): CompletionResult<UserWallet?> {
        return keysRepository.getAll()
            .flatMap { encryptionKeys ->
                loadUserWallets(
                    encryptionKeys = state.value.encryptionKeys
                        .plus(encryptionKeys)
                        .distinctBy(UserWalletEncryptionKey::walletId),
                )
            }
            .map { loadedState ->
                when (type) {
                    UnlockType.ALL -> {
                        if (loadedState.isLocked) {
                            Timber.e("Some user wallets remain locked")

                            state.value = loadedState

                            throw UserWalletsListError.NotAllUserWalletsUnlocked
                        } else {
                            val prevState = state.value

                            val selectedWallet = findOrSetSelectedWallet(
                                prevSelectedWalletId = prevState.selectedUserWalletId,
                                userWallets = loadedState.userWallets,
                                prevSelectedWalletIndex = prevState.userWallets.indexOfFirst {
                                    it.walletId == prevState.selectedUserWalletId
                                },
                            )

                            state.value = loadedState.copy(selectedUserWalletId = selectedWallet?.walletId)

                            selectedWallet
                        }
                    }
                    UnlockType.ANY -> {
                        val prevState = state.value

                        val selectedWallet = findOrSetSelectedWallet(
                            prevSelectedWalletId = state.value.selectedUserWalletId,
                            prevSelectedWalletIndex = prevState.userWallets.indexOfFirst {
                                it.walletId == prevState.selectedUserWalletId
                            },
                            userWallets = loadedState.userWallets,
                        )

                        state.value = loadedState.copy(selectedUserWalletId = selectedWallet?.walletId)

                        selectedWallet
                    }
                    UnlockType.ALL_WITHOUT_SELECT -> {
                        state.value = loadedState

                        findSelectedUserWallet()
                    }
                }
            }
    }

    private suspend fun loadUserWallets(encryptionKeys: List<UserWalletEncryptionKey>): CompletionResult<State> {
        return publicInformationRepository.getAll()
            .map { it.toUserWallets() }
            .flatMap { userWallets ->
                sensitiveInformationRepository.getAll(encryptionKeys)
                    .map { walletIdToSensitiveInformation ->
                        userWallets.updateWith(walletIdToSensitiveInformation)
                    }
            }
            .map { userWallets ->
                val prevState = state.value

                if (userWallets.isNotEmpty()) {
                    val newUserWallets = (userWallets + prevState.userWallets)
                        .distinctBy(UserWallet::walletId)

                    prevState.copy(
                        userWallets = newUserWallets,
                        encryptionKeys = encryptionKeys,
                        isLocked = newUserWallets.any(UserWallet::isLocked),
                    )
                } else {
                    prevState
                }
            }
    }

    private fun findOrSetSelectedWallet(
        prevSelectedWalletId: UserWalletId?,
        prevSelectedWalletIndex: Int,
        userWallets: List<UserWallet>,
        ignorePrevSelectedWallet: Boolean = false,
    ): UserWallet? {
        var possibleSelectedUserWallet: UserWallet? = null

        if (!ignorePrevSelectedWallet) {
            val selectedWalletId = prevSelectedWalletId ?: selectedUserWalletRepository.get()
            possibleSelectedUserWallet = findSelectedUserWallet(userWallets, selectedWalletId)
        }

        if (possibleSelectedUserWallet == null || possibleSelectedUserWallet.isLocked) {
            possibleSelectedUserWallet =
                userWallets.findAvailableUserWallet(prevSelectedIndex = prevSelectedWalletIndex)
        }

        selectedUserWalletRepository.set(possibleSelectedUserWallet?.walletId)

        return possibleSelectedUserWallet
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

    private fun findSelectedUserWallet(
        userWallets: List<UserWallet> = state.value.userWallets,
        selectedUserWalletId: UserWalletId? = state.value.selectedUserWalletId,
    ): UserWallet? {
        return userWallets.firstOrNull { it.walletId == selectedUserWalletId }
    }

    private data class State(
        val encryptionKeys: List<UserWalletEncryptionKey> = emptyList(),
        val userWallets: List<UserWallet> = emptyList(),
        val selectedUserWalletId: UserWalletId? = null,
        val isLocked: Boolean = true,
    )
}