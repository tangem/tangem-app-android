package com.tangem.tap.domain.walletStores.implementation

import com.tangem.blockchain.common.WalletManager
import com.tangem.common.CompletionResult
import com.tangem.common.flatMap
import com.tangem.common.flatMapOnFailure
import com.tangem.common.map
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.domain.extensions.isMultiwalletAllowed
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.model.builders.WalletStoreBuilder
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.domain.walletStores.WalletStoresError
import com.tangem.tap.domain.walletStores.WalletStoresManager
import com.tangem.tap.domain.walletStores.implementation.utils.fold
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.WalletManagersRepository
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository
import com.tangem.tap.features.wallet.models.toBlockchainNetworks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal class DefaultWalletStoresManager(
    private val userTokensRepository: UserTokensRepository,
    private val walletStoresRepository: WalletStoresRepository,
    private val walletAmountsRepository: WalletAmountsRepository,
    private val walletManagersRepository: WalletManagersRepository,
    private val appCurrencyProvider: () -> FiatCurrency,
) : WalletStoresManager {
    private val state = MutableStateFlow(State())

    override fun getAll(): Flow<Map<UserWalletId, List<WalletStoreModel>>> {
        return walletStoresRepository.getAll()
    }

    override fun get(userWalletId: UserWalletId): Flow<List<WalletStoreModel>> {
        return walletStoresRepository.get(userWalletId)
    }

    override suspend fun delete(userWalletsIds: List<String>): CompletionResult<Unit> {
        val walletIds = userWalletsIds.map { UserWalletId(it) }
        return walletStoresRepository.delete(walletIds)
            .flatMap { walletManagersRepository.delete(walletIds) }
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return walletStoresRepository.clear()
    }

    override suspend fun fetch(
        userWallets: List<UserWallet>,
        refresh: Boolean,
    ): CompletionResult<Unit> {
        val fiatCurrency = appCurrencyProvider.invoke()
        val isFiatCurrencyChanged = state.value.fiatCurrency != fiatCurrency

        state.update { prevState ->
            prevState.copy(
                fiatCurrency = fiatCurrency,
            )
        }

        return userWallets
            .mapNotNull { userWallet ->
                val hasNotWalletStoresForUserWallet = !walletStoresRepository.contains(userWallet.walletId)
                if (refresh || hasNotWalletStoresForUserWallet || isFiatCurrencyChanged) {
                    fetchWalletsIfNeeded(userWallet, refresh)
                } else null
            }
            .fold(initial = arrayListOf<UserWallet>()) { acc, data ->
                acc.apply { add(data) }
            }
            .flatMap {
                walletAmountsRepository.update(it, fiatCurrency)
            }
    }

    override suspend fun fetch(
        userWallet: UserWallet,
        refresh: Boolean,
    ): CompletionResult<Unit> {
        return fetch(listOf(userWallet), refresh)
    }

    private suspend fun fetchWalletsIfNeeded(
        userWallet: UserWallet,
        refresh: Boolean,
    ): CompletionResult<UserWallet> {
        return if (userWallet.scanResponse.card.isMultiwalletAllowed) {
            fetchMultiWallets(userWallet, refresh)
        } else {
            fetchSingleWallet(userWallet, refresh)
        }
            .map { userWallet }
    }

    private suspend fun fetchMultiWallets(
        userWallet: UserWallet,
        refresh: Boolean,
    ): CompletionResult<Unit> {
        val scanResponse = userWallet.scanResponse
        val userTokens = withContext(Dispatchers.IO) {
            userTokensRepository.getUserTokens(scanResponse.card)
        }
        val userWalletId = userWallet.walletId

        return withContext(Dispatchers.Default) {
            userTokens.toBlockchainNetworks()
                .also { blockchainNetworks ->
                    walletStoresRepository.deleteDifference(
                        userWalletId = userWalletId,
                        currentBlockchains = blockchainNetworks.map { it.blockchain },
                    )
                }
                .map { blockchainNetwork ->
                    val storeWalletStore: suspend (WalletManager?) -> CompletionResult<Unit> =
                        { walletManager ->
                            walletStoresRepository.storeOrUpdate(
                                userWalletId = userWalletId,
                                walletStore = WalletStoreBuilder(userWalletId, blockchainNetwork)
                                    .walletManager(walletManager)
                                    .build(),
                            )
                        }

                    walletManagersRepository.findOrMake(
                        userWallet = userWallet,
                        blockchainNetwork = blockchainNetwork,
                        refresh = refresh,
                    )
                        .flatMap { walletManager ->
                            storeWalletStore(walletManager)
                        }
                        .flatMapOnFailure { error ->
                            when (error) {
                                is WalletStoresError.WalletManagerNotCreated,
                                is WalletStoresError.UpdateWalletManagerError,
                                -> storeWalletStore(null)
                                else -> CompletionResult.Failure(error)
                            }
                        }
                }
                .fold()
        }
    }

    private suspend fun fetchSingleWallet(
        userWallet: UserWallet,
        refresh: Boolean,
    ): CompletionResult<Unit> {
        return walletManagersRepository.findOrMake(
            userWallet = userWallet,
            refresh = refresh,
        )
            .flatMap { walletManager ->
                val userWalletId = userWallet.walletId
                walletStoresRepository.storeOrUpdate(
                    userWalletId = userWalletId,
                    walletStore = WalletStoreBuilder(userWalletId, walletManager)
                        .build(),
                )
            }
            .flatMapOnFailure { error ->
                when (error) {
                    is WalletStoresError.WalletManagerNotCreated,
                    is WalletStoresError.UpdateWalletManagerError,
                    -> CompletionResult.Success(Unit)
                    else -> CompletionResult.Failure(error)
                }
            }
    }

    internal data class State(
        val fiatCurrency: FiatCurrency? = null,
    )
}
