package com.tangem.tap.domain.userWalletList.implementation

import com.tangem.common.*
import com.tangem.domain.common.util.UserWalletId
import com.tangem.domain.common.util.encryptionKey
import com.tangem.tap.domain.TangemSdkManager
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
    private val tangemSdkManager: TangemSdkManager,
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
                state.wallets.find {
                    it.walletId == state.selectedWalletId
                }
            }
            .filterNotNull()
            .distinctUntilChanged()

    override val selectedUserWalletSync: UserWallet?
        get() = findSelectedWallet()

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

    override suspend fun unlockWithCard(userWallet: UserWallet): CompletionResult<Unit> {
        state.update { prevState ->
            userWallet.isSaved = false
            prevState.copy(
                encryptionKeys = listOf(UserWalletEncryptionKey(userWallet)),
                wallets = listOf(userWallet),
            )
        }

        return loadModels()
            .map {
                state.update { prevState ->
                    prevState.copy(
                        selectedWalletId = userWallet.walletId,
                        isLocked = prevState.wallets.size != 1,
                    )
                }
            }
    }

    override fun lock() {
        tangemSdkManager.biometricManager.unauthenticate()
        state.update { prevState ->
            prevState.copy(
                encryptionKeys = emptyList(),
                isLocked = true,
            )
        }
    }

    override suspend fun selectWallet(walletId: UserWalletId): CompletionResult<UserWallet> = catching {
        if (state.value.selectedWalletId == walletId) {
            return@catching findSelectedWallet()!!
        }

        if (!state.value.isLocked) {
            selectedUserWalletRepository.set(walletId)

            state.update { prevState ->
                prevState.copy(
                    selectedWalletId = walletId,
                )
            }
        }

        findSelectedWallet()!!
    }

    override suspend fun save(
        userWallet: UserWallet,
        canOverride: Boolean,
    ): CompletionResult<Unit> = withUnlock {
        val isWalletSaved = state.value.wallets
            .filter { it.isSaved }
            .flatMap(UserWallet::cardsInWallet)
            .contains(userWallet.cardId)

        if (isWalletSaved && !canOverride) {
            CompletionResult.Failure(UserWalletListError.WalletAlreadySaved)
        } else {
            keysRepository.save(
                walletId = userWallet.walletId,
                encryptionKey = userWallet.scanResponse.card.encryptionKey,
            )
                .doOnSuccess { keys ->
                    state.update { prevState ->
                        prevState.copy(
                            encryptionKeys = (keys + prevState.encryptionKeys).distinctBy { it.walletId },
                        )
                    }
                }
                .flatMap { publicInformationRepository.save(userWallet) }
                .flatMap { sensitiveInformationRepository.save(userWallet) }
                .flatMap { loadModels() }
                .doOnSuccess {
                    userWallet.isSaved = true
                }
        }
    }

    override suspend fun delete(walletIds: List<UserWalletId>): CompletionResult<Unit> {
        val walletIdsToRemove = state.value.wallets
            .map { it.walletId }
            .filter { it in walletIds }

        changeSelectedWalletIfNeeded(walletIdsToRemove)

        return sensitiveInformationRepository.delete(walletIdsToRemove)
            .flatMap { publicInformationRepository.delete(walletIdsToRemove) }
            .flatMap { keysRepository.delete(walletIdsToRemove) }
            .map { keys ->
                state.update { prevState ->
                    prevState.copy(
                        encryptionKeys = keys,
                        wallets = prevState.wallets.filter { it.walletId !in walletIdsToRemove },
                    )
                }
            }
            .flatMap { loadModels() }
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return sensitiveInformationRepository.delete(
            walletIds = state.value.wallets.map { it.walletId },
        )
            .flatMap { publicInformationRepository.clear() }
            .flatMap { keysRepository.clear() }
            .map {
                selectedUserWalletRepository.set(null)
                tangemSdkManager.biometricManager.unauthenticate()
                state.update { State() }
            }
    }

    override suspend fun get(walletId: UserWalletId): CompletionResult<UserWallet> = withUnlock {
        return catching {
            state.value.wallets.first { it.walletId == walletId }
        }
    }

    private suspend inline fun <reified T> withUnlock(
        block: () -> CompletionResult<T>,
    ): CompletionResult<T> {
        return (if (state.value.isLocked) unlockWithBiometryInternal() else CompletionResult.Success(Unit))
            .flatMap { block() }
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
                        selectedWalletId = findOrSetSelectedWallet(prevState.selectedWalletId, wallets),
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

    private fun findOrSetSelectedWallet(
        prevSelectedWalletId: UserWalletId?,
        userWallets: List<UserWallet>,
    ): UserWalletId? {
        return prevSelectedWalletId
            ?: (selectedUserWalletRepository.get()
                ?: (userWallets.firstOrNull()?.walletId
                    ?.also { selectedUserWalletRepository.set(it) }))
    }

    private fun changeSelectedWalletIfNeeded(
        walletsIdsToRemove: List<UserWalletId>,
    ) {
        val remainingWallets = state.value.wallets.filter {
            it.walletId !in walletsIdsToRemove
        }
        val selectedWallet = findSelectedWallet()
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

    private fun findSelectedWallet(): UserWallet? {
        return with(state.value) {
            wallets.find {
                it.walletId == selectedWalletId
            }
        }
    }

    private data class State(
        val encryptionKeys: List<UserWalletEncryptionKey> = emptyList(),
        val wallets: List<UserWallet> = emptyList(),
        val selectedWalletId: UserWalletId? = null,
        val isLocked: Boolean = true,
    )
}
