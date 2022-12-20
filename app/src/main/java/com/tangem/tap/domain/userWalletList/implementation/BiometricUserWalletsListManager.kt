package com.tangem.tap.domain.userWalletList.implementation

import com.tangem.common.*
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.isTwinnedWith
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
            .mapLatest { it.wallets }
            .distinctUntilChanged()

    override val selectedUserWallet: Flow<UserWallet>
        get() = state
            .mapLatest { state ->
                findSelectedUserWallet(state.wallets)
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
        get() = selectedUserWalletRepository.get() != null

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

    override suspend fun selectWallet(walletId: UserWalletId): CompletionResult<UserWallet> = catching {
        if (state.value.selectedWalletId == walletId) {
            return@catching findSelectedUserWallet()!!
        }

        if (!state.value.isLocked) {
            selectedUserWalletRepository.set(walletId)

            state.update { prevState ->
                prevState.copy(
                    selectedWalletId = walletId,
                )
            }
        }

        findSelectedUserWallet()!!
    }

    override suspend fun save(userWallet: UserWallet, canOverride: Boolean): CompletionResult<Unit> {
        return if (canOverride) {
            saveInternal(userWallet)
        } else {
            val isWalletSaved = state.value.wallets
                .any {
                    // Workaround, check [UserWallet.isTwinnedWith]
                    it.cardsInWallet.contains(userWallet.cardId) || it.isTwinnedWith(userWallet)
                }

            if (isWalletSaved) {
                CompletionResult.Failure(UserWalletListError.WalletAlreadySaved)
            } else {
                saveInternal(userWallet)
            }
        }
    }

    override suspend fun delete(walletIds: List<UserWalletId>): CompletionResult<Unit> {
        if (walletIds.isEmpty()) {
            return CompletionResult.Success(Unit)
        }

        val walletIdsToRemove = state.value.wallets
            .map { it.walletId }
            .filter { it in walletIds }
        val remainingEncryptionKeys = state.value.encryptionKeys
            .filter { it.walletId !in walletIdsToRemove }

        changeSelectedUserWalletIdIfNeeded(walletIdsToRemove)

        return sensitiveInformationRepository.delete(walletIdsToRemove)
            .flatMap { publicInformationRepository.delete(walletIdsToRemove) }
            .flatMap { keysRepository.delete(walletIdsToRemove) }
            .map {
                state.update { prevState ->
                    prevState.copy(
                        encryptionKeys = remainingEncryptionKeys,
                        wallets = prevState.wallets.filter { it.walletId !in walletIdsToRemove },
                    )
                }
            }
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return sensitiveInformationRepository.delete(
            walletIds = state.value.wallets.map { it.walletId },
        )
            .flatMap { publicInformationRepository.clear() }
            .flatMap { keysRepository.clear() }
            .map {
                selectedUserWalletRepository.set(null)
                state.update { State() }
            }
    }

    override suspend fun get(walletId: UserWalletId): CompletionResult<UserWallet> {
        return catching {
            state.value.wallets.first { it.walletId == walletId }
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
                        selectedWalletId = userWallet.walletId,
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
                        isLocked = prevState.wallets.any { it.isLocked },
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
                    val wallets = (userWallets + prevState.wallets).distinctBy { it.walletId }

                    prevState.copy(
                        wallets = wallets,
                        selectedWalletId = findOrSetSelectedUserWalletId(prevState.selectedWalletId, wallets),
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
        return prevSelectedWalletId
            ?: (selectedUserWalletRepository.get()
                ?: (userWallets.firstOrNull()?.walletId?.also { selectedUserWalletRepository.set(it) }))
    }

    private fun changeSelectedUserWalletIdIfNeeded(walletsIdsToRemove: List<UserWalletId>) {
        val remainingWallets = state.value.wallets.filter {
            it.walletId !in walletsIdsToRemove
        }
        val selectedWallet = findSelectedUserWallet()
        when {
            remainingWallets.isEmpty() -> {
                state.update { prevState ->
                    prevState.copy(
                        selectedWalletId = null,
                    )
                }
                selectedUserWalletRepository.set(null)
            }
            !remainingWallets.contains(selectedWallet) -> {
                val newSelectedWallet = remainingWallets.first()
                state.update { prevState ->
                    prevState.copy(
                        selectedWalletId = newSelectedWallet.walletId,
                    )
                }
                selectedUserWalletRepository.set(newSelectedWallet.walletId)
            }
        }
    }

    private fun findSelectedUserWallet(userWallets: List<UserWallet> = state.value.wallets): UserWallet? {
        return userWallets.find {
            it.walletId == state.value.selectedWalletId
        }
    }

    private data class State(
        val encryptionKeys: List<UserWalletEncryptionKey> = emptyList(),
        val wallets: List<UserWallet> = emptyList(),
        val selectedWalletId: UserWalletId? = null,
        val isLocked: Boolean = true,
    )
}
