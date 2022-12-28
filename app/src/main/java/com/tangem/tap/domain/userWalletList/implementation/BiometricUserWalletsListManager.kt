package com.tangem.tap.domain.userWalletList.implementation

import com.tangem.common.*
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.userWalletList.UserWalletListError
import com.tangem.tap.domain.userWalletList.UserWalletsListManager
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.repository.SelectedUserWalletRepository
import com.tangem.tap.domain.userWalletList.repository.UserWalletsKeysRepository
import com.tangem.tap.domain.userWalletList.repository.UserWalletsPublicInformationRepository
import com.tangem.tap.domain.userWalletList.repository.UserWalletsSensitiveInformationRepository
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
) : UserWalletsListManager {
    private val state = MutableStateFlow(State())

    override val userWallets: Flow<List<UserWallet>>
        get() = state
            .mapLatest { it.userWallets }
            .distinctUntilChanged()

    override val selectedUserWallet: Flow<UserWallet>
        get() = state
            .mapLatest { state ->
                findSelectedUserWallet(state.userWallets)
            }
            .filterNotNull()
            .distinctUntilChanged()

    override val selectedUserWalletSync: UserWallet?
        get() = findSelectedUserWallet()

    override val isLocked: Flow<Boolean>
        get() = state
            .mapLatest { it.isLocked }
            .distinctUntilChanged()

    override val isLockedSync: Boolean
        get() = state.value.isLocked

    override val hasSavedUserWallets: Boolean
        get() = publicInformationRepository.isNotEmpty()

    override suspend fun unlockWithBiometry(): CompletionResult<UserWallet?> {
        return unlockWithBiometryInternal()
            .map { selectedUserWalletSync }
    }

    override fun lock() {
        state.update { prevState ->
            prevState.copy(
                encryptionKeys = emptyList(),
                isLocked = true,
            )
        }
    }

    override suspend fun selectWallet(userWalletId: UserWalletId): CompletionResult<UserWallet> = catching {
        if (state.value.selectedUserWalletId == userWalletId) {
            return@catching findSelectedUserWallet()!!
        }

        selectedUserWalletRepository.set(userWalletId)

        state.update { prevState ->
            prevState.copy(
                selectedUserWalletId = userWalletId,
            )
        }

        findSelectedUserWallet()!!
    }

    override suspend fun save(userWallet: UserWallet, canOverride: Boolean): CompletionResult<Unit> {
        return if (canOverride) {
            saveInternal(userWallet)
        } else {
            val isWalletSaved = state.value.userWallets
                .any {
                    it.walletId == userWallet.walletId || it.cardsInWallet.contains(userWallet.cardId)
                }

            if (isWalletSaved) {
                CompletionResult.Failure(UserWalletListError.WalletAlreadySaved)
            } else {
                saveInternal(userWallet)
            }
        }
    }

    override suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit> {
        if (userWalletIds.isEmpty()) {
            return CompletionResult.Success(Unit)
        }

        changeSelectedUserWalletIdIfNeeded(userWalletIds)

        return sensitiveInformationRepository.delete(userWalletIds)
            .flatMap { publicInformationRepository.delete(userWalletIds) }
            .flatMap { keysRepository.delete(userWalletIds) }
            .map {
                state.update { prevState ->
                    prevState.copy(
                        encryptionKeys = prevState.encryptionKeys.filter { it.walletId !in userWalletIds },
                        userWallets = prevState.userWallets.filter { it.walletId !in userWalletIds },
                    )
                }
            }
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return sensitiveInformationRepository.clear()
            .flatMap { publicInformationRepository.clear() }
            .flatMap { keysRepository.clear() }
            .map {
                selectedUserWalletRepository.set(null)
                state.update { State() }
            }
    }

    override suspend fun get(userWalletId: UserWalletId): CompletionResult<UserWallet> {
        return catching {
            state.value.userWallets.first { it.walletId == userWalletId }
        }
    }

    private suspend fun saveInternal(userWallet: UserWallet): CompletionResult<Unit> {
        val newEncryptionKeys = state.value.encryptionKeys
            .plus(UserWalletEncryptionKey(userWallet))
            .distinctBy { it.walletId }

        return keysRepository.store(newEncryptionKeys)
            .doOnSuccess {
                state.update { prevState ->
                    prevState.copy(
                        encryptionKeys = newEncryptionKeys,
                        selectedUserWalletId = userWallet.walletId,
                    )
                }
            }
            .flatMap { publicInformationRepository.save(userWallet) }
            .flatMap { sensitiveInformationRepository.save(userWallet) }
            .map { selectedUserWalletRepository.set(userWallet.walletId) }
            .flatMap { loadModels() }
            .doOnSuccess {
                state.update { prevState ->
                    prevState.copy(
                        isLocked = prevState.userWallets.any { it.isLocked },
                    )
                }
            }
    }

    private suspend fun unlockWithBiometryInternal(): CompletionResult<Unit> {
        return keysRepository.getAll()
            .map { keys ->
                state.update { prevState ->
                    prevState.copy(
                        encryptionKeys = (keys + prevState.encryptionKeys).distinctBy { it.walletId },
                    )
                }
            }
            .flatMap { loadModels() }
            .map {
                state.update { prevState ->
                    prevState.copy(
                        isLocked = false,
                    )
                }
            }
    }

    private suspend fun loadModels(): CompletionResult<Unit> {
        return getSavedUserWallets()
            .map { userWallets ->
                if (userWallets.isNotEmpty()) state.update { prevState ->
                    val wallets = (userWallets + prevState.userWallets).distinctBy { it.walletId }

                    prevState.copy(
                        userWallets = wallets,
                        selectedUserWalletId = findOrSetSelectedUserWalletId(prevState.selectedUserWalletId, wallets),
                    )
                }
            }
            .doOnFailure { error ->
                Timber.e(error, "Unable to load user wallets")
            }
    }

    private suspend fun getSavedUserWallets(): CompletionResult<List<UserWallet>> {
        return publicInformationRepository.getAll()
            .map { it.toUserWallets() }
            .flatMap { userWallets ->
                sensitiveInformationRepository.getAll(state.value.encryptionKeys)
                    .map { walletIdToSensitiveInformation ->
                        userWallets.updateWith(walletIdToSensitiveInformation)
                    }
            }
    }

    private fun findOrSetSelectedUserWalletId(
        prevSelectedWalletId: UserWalletId?,
        userWallets: List<UserWallet>,
    ): UserWalletId? {
        val findUnlockedAndSet = {
            userWallets.firstOrNull { !it.isLocked }
                ?.walletId
                ?.also { selectedUserWalletRepository.set(it) }
        }

        return prevSelectedWalletId ?: (selectedUserWalletRepository.get() ?: findUnlockedAndSet())
    }

    private fun changeSelectedUserWalletIdIfNeeded(walletsIdsToRemove: List<UserWalletId>) {
        val remainingWallets = state.value.userWallets.filter {
            it.walletId !in walletsIdsToRemove
        }
        val selectedWallet = findSelectedUserWallet()
        when {
            remainingWallets.isEmpty() -> {
                state.update { prevState ->
                    prevState.copy(
                        selectedUserWalletId = null,
                    )
                }
                selectedUserWalletRepository.set(null)
            }
            !remainingWallets.contains(selectedWallet) -> {
                val newSelectedWallet = remainingWallets.firstOrNull { !it.isLocked }
                state.update { prevState ->
                    prevState.copy(
                        selectedUserWalletId = newSelectedWallet?.walletId,
                    )
                }
                selectedUserWalletRepository.set(newSelectedWallet?.walletId)
            }
        }
    }

    private fun findSelectedUserWallet(userWallets: List<UserWallet> = state.value.userWallets): UserWallet? {
        return userWallets.find {
            it.walletId == state.value.selectedUserWalletId
        }
    }

    private data class State(
        val encryptionKeys: List<UserWalletEncryptionKey> = emptyList(),
        val userWallets: List<UserWallet> = emptyList(),
        val selectedUserWalletId: UserWalletId? = null,
        val isLocked: Boolean = true,
    )
}
