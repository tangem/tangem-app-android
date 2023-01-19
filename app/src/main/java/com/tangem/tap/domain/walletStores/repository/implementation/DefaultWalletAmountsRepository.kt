package com.tangem.tap.domain.walletStores.repository.implementation

import com.tangem.blockchain.blockchains.solana.RentProvider
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result.Failure
import com.tangem.blockchain.extensions.Result.Success
import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.common.core.TangemError
import com.tangem.common.flatMap
import com.tangem.common.flatMapOnFailure
import com.tangem.common.fold
import com.tangem.common.map
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.replaceByOrAdd
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.walletStores.WalletStoresError
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.implementation.utils.replaceWalletStore
import com.tangem.tap.domain.walletStores.repository.implementation.utils.replaceWalletStores
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithAmounts
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithError
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithFiatRates
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithMissedDerivation
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithRent
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithUnreachable
import com.tangem.tap.domain.walletStores.storage.WalletManagerStorage
import com.tangem.tap.domain.walletStores.storage.WalletStoresStorage
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.tap.features.wallet.models.filterByCoin
import com.tangem.tap.features.wallet.models.getPendingTransactions
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal

@Suppress("LargeClass")
internal class DefaultWalletAmountsRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletAmountsRepository {
    private val walletStoresStorage = WalletStoresStorage
    private val walletManagersStorage = WalletManagerStorage

    override suspend fun updateAmountsForUserWallets(
        userWallets: List<UserWallet>,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit> {
        return if (userWallets.isEmpty()) {
            CompletionResult.Success(Unit)
        } else {
            withContext(Dispatchers.Default) {
                awaitAll(
                    async { fetchAmountsForUserWallets(userWallets) },
                    async { fetchFiatRates(userWallets, walletStores = null, fiatCurrency) },
                )
                    .fold()
            }
        }
    }

    override suspend fun updateAmountsForUserWallet(
        userWallet: UserWallet,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit> {
        return updateAmountsForUserWallets(listOf(userWallet), fiatCurrency)
    }

    override suspend fun updateAmountsForWalletStores(
        walletStores: List<WalletStoreModel>,
        userWallet: UserWallet,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit> {
        return if (walletStores.isEmpty()) {
            CompletionResult.Success(Unit)
        } else {
            withContext(Dispatchers.Default) {
                val userWalletId = userWallet.walletId
                val scanResponse = userWallet.scanResponse

                awaitAll(
                    async { fetchAmountForWalletStores(userWalletId, scanResponse, walletStores) },
                    async { fetchFiatRates(listOf(userWallet), walletStores, fiatCurrency) },
                )
                    .fold()
            }
        }
    }

    override suspend fun updateAmountsForWalletStore(
        walletStore: WalletStoreModel,
        userWallet: UserWallet,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit> {
        return updateAmountsForWalletStores(listOf(walletStore), userWallet, fiatCurrency)
    }

    private suspend fun fetchFiatRates(
        userWallets: List<UserWallet>,
        walletStores: List<WalletStoreModel>?,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit> {
        val walletStoresInternal = walletStores ?: getWalletStores(userWallets)

        val currencies = walletStoresInternal
            .asSequence()
            .flatMap { it.walletsData }
            .map { it.currency }

        val coinsIds = currencies.mapNotNull { it.coinId }.distinct().toList()

        return withContext(dispatchers.io) {
            runCatching { tangemTechApi.getRates(fiatCurrency.code.lowercase(), coinsIds.joinToString(",")) }
                .onSuccess {
                    updateWalletStoresWithFiatRates(walletStores = walletStoresInternal, fiatRates = it.rates)
                    return@withContext CompletionResult.Success(Unit)
                }
                .onFailure {
                    val error = WalletStoresError.FetchFiatRatesError(
                        currencies = currencies.map(Currency::currencySymbol).toList(),
                        cause = it,
                    )

                    Timber.e(
                        error,
                        """
                        Unable to fetch fiat rates
                        |- Coins ids: $coinsIds
                        """.trimIndent(),
                    )

                    return@withContext CompletionResult.Failure(error)
                }

            error("Unreachable code because runCatching must return result")
        }
    }

    private suspend fun fetchAmountsForUserWallets(
        userWallets: List<UserWallet>,
    ): CompletionResult<Unit> = withContext(Dispatchers.Default) {
        userWallets.map { async { fetchAmountsForUserWallet(it) } }
            .awaitAll()
            .fold()
    }

    private suspend fun fetchAmountsForUserWallet(
        userWallet: UserWallet,
    ): CompletionResult<Unit> = withContext(Dispatchers.Default) {
        val userWalletId = userWallet.walletId
        val scanResponse = userWallet.scanResponse
        val walletStores = getWalletStores(listOf(userWallet))

        fetchAmountForWalletStores(userWalletId, scanResponse, walletStores)
    }

    private suspend fun fetchAmountForWalletStores(
        userWalletId: UserWalletId,
        scanResponse: ScanResponse,
        walletStores: List<WalletStoreModel>,
    ): CompletionResult<Unit> = coroutineScope {
        walletStores.map { walletStore ->
            async {
                // TODO: Find wallet manager via [com.tangem.tap.domain.walletStores.repository.WalletManagersRepository]
                val walletManager = walletStore.walletManager
                fetchAmountsForWalletStore(userWalletId, scanResponse, walletStore, walletManager)
            }
        }
            .awaitAll()
            .fold()
    }

    private suspend fun fetchAmountsForWalletStore(
        userWalletId: UserWalletId,
        scanResponse: ScanResponse,
        walletStore: WalletStoreModel,
        walletManager: WalletManager?,
    ): CompletionResult<Unit> {
        val hasMissedDerivations = with(walletStore) {
            derivationPath != null && !scanResponse.hasDerivation(blockchain, derivationPath.rawPath)
        }

        return when {
            hasMissedDerivations -> {
                updateWalletStoreWithMissedDerivation(walletStore)
            }
            walletManager == null -> {
                updateWalletStoreWithUnreachable(walletStore)
            }
            else -> {
                withInternetConnection { walletManager.update() }
                    .map { updateWalletManagerWithAmounts(userWalletId, walletManager) }
                    .flatMap {
                        updateWalletStoreWithAmounts(
                            walletStore = walletStore,
                            updatedWallet = walletManager.wallet,
                        )
                    }
                    .flatMap { fetchWalletStoreRentIfNeeded(walletStore, walletManager) }
                    .flatMapOnFailure { error ->
                        updateWalletStoreWithError(
                            walletStore = walletStore,
                            wallet = walletManager.wallet,
                            error = error,
                        )
                    }
            }
        }
    }

    private suspend fun fetchWalletStoreRentIfNeeded(
        walletStore: WalletStoreModel,
        walletManager: WalletManager,
    ): CompletionResult<Unit> {
        val rentProvider = walletManager as? RentProvider
            ?: return CompletionResult.Success(Unit)

        when (val result = rentProvider.minimalBalanceForRentExemption()) {
            is Success -> {
                val balance = walletManager.wallet.fundsAvailable(AmountType.Coin)
                val outgoingTxs = walletManager.wallet.getPendingTransactions(
                    PendingTransactionType.Outgoing,
                ).filterByCoin()

                val rentExempt = result.data
                val setRent = if (outgoingTxs.isEmpty()) {
                    balance < rentExempt
                } else {
                    val outgoingAmount = outgoingTxs.sumOf { it.amountValue ?: BigDecimal.ZERO }
                    val rest = balance.minus(outgoingAmount)
                    balance < rest
                }

                updateWalletStoreWithRent(
                    walletStore = walletStore,
                    rent = if (setRent) {
                        WalletStoreModel.WalletRent(
                            rent = rentProvider.rentAmount(),
                            exemptionAmount = rentExempt,
                        )
                    } else null,
                )
            }
            is Failure -> Unit
        }

        return CompletionResult.Success(Unit)
    }

    private suspend fun updateWalletStoreWithError(
        walletStore: WalletStoreModel,
        wallet: Wallet,
        error: TangemError,
    ) = withContext(Dispatchers.Default) {
        Timber.e(
            error,
            """
                Unable to fetch amounts
                |- User wallet id: ${walletStore.userWalletId}
                |- Blockchain: ${walletStore.blockchain}
            """.trimIndent(),
        )

        if (error is BlockchainSdkError) {
            walletStoresStorage.update { prevState ->
                prevState.replaceWalletStore(
                    walletStoreToUpdate = walletStore,
                    update = {
                        it.updateWithError(
                            wallet = wallet,
                            error = error,
                        )
                    },
                )
            }

            CompletionResult.Success(Unit)
        } else {
            CompletionResult.Failure(error)
        }
    }

    private suspend fun updateWalletStoreWithAmounts(
        walletStore: WalletStoreModel,
        updatedWallet: Wallet,
    ) = withContext(Dispatchers.Default) {
        Timber.d(
            """
                Fetched amounts
                |- User wallet id: ${walletStore.userWalletId}
                |- Blockchain: ${walletStore.blockchain}
            """.trimIndent(),
        )

        walletStoresStorage.update { prevState ->
            prevState.replaceWalletStore(
                walletStoreToUpdate = walletStore,
                update = {
                    it.updateWithAmounts(wallet = updatedWallet)
                },
            )
        }

        CompletionResult.Success(Unit)
    }

    private suspend fun updateWalletStoreWithMissedDerivation(
        walletStore: WalletStoreModel,
    ) = withContext(Dispatchers.Default) {
        Timber.e(
            """
                Missed derivation
                |- User wallet id: ${walletStore.userWalletId}
                |- Blockchain: ${walletStore.blockchain}
            """.trimIndent(),
        )

        walletStoresStorage.update { prevState ->
            prevState.replaceWalletStore(
                walletStoreToUpdate = walletStore,
                update = {
                    it.updateWithMissedDerivation()
                },
            )
        }

        CompletionResult.Success(Unit)
    }

    private suspend fun updateWalletStoreWithUnreachable(
        walletStore: WalletStoreModel,
    ) = withContext(Dispatchers.Default) {
        Timber.e(
            """
                Wallet manager is null
                |- User wallet id: ${walletStore.userWalletId}
                |- Blockchain: ${walletStore.blockchain}
            """.trimIndent(),
        )

        walletStoresStorage.update { prevState ->
            prevState.replaceWalletStore(
                walletStoreToUpdate = walletStore,
                update = {
                    it.updateWithUnreachable()
                },
            )
        }

        CompletionResult.Success(Unit)
    }

    private suspend fun updateWalletStoresWithFiatRates(
        walletStores: List<WalletStoreModel>,
        fiatRates: Map<String, Double>,
    ) = withContext(Dispatchers.Default) {
        Timber.d(
            """
                Fetched fiat rates
                |- User wallets ids: ${walletStores.map { it.userWalletId }.distinct()}
            """.trimIndent(),
        )

        walletStoresStorage.update { prevState ->
            prevState.replaceWalletStores(
                walletStoresToUpdate = walletStores,
                update = {
                    it.updateWithFiatRates(rates = fiatRates)
                },
            )
        }
    }

    private suspend fun updateWalletStoreWithRent(
        walletStore: WalletStoreModel,
        rent: WalletStoreModel.WalletRent?,
    ) = withContext(Dispatchers.Default) {
        Timber.d(
            """
                Fetched wallet rent
                |- User wallet id: ${walletStore.userWalletId}
                |- Blockchain: ${walletStore.blockchain}
                |- Rent: $rent
            """.trimIndent(),
        )

        if (rent != walletStore.walletRent) {
            walletStoresStorage.update { prevState ->
                prevState.replaceWalletStore(
                    walletStoreToUpdate = walletStore,
                    update = {
                        it.updateWithRent(rent)
                    },
                )
            }
        }
    }

    private suspend inline fun withInternetConnection(crossinline block: suspend () -> Unit): CompletionResult<Unit> {
        return if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
            val error = WalletStoresError.NoInternetConnection
            Timber.e(error)
            CompletionResult.Failure(error)
        } else {
            withContext(Dispatchers.IO) {
                catching { block() }
            }
        }
    }

    private suspend fun updateWalletManagerWithAmounts(
        userWalletId: UserWalletId,
        walletManager: WalletManager,
    ) = withContext(Dispatchers.Default) {
        walletManagersStorage.update { prevManagers ->
            val newManagersForUserWallet = prevManagers[userWalletId].orEmpty()
                .toMutableList()
                .apply {
                    replaceByOrAdd(walletManager) {
                        it.wallet.blockchain == it.wallet.blockchain
                    }
                }

            prevManagers.apply {
                set(userWalletId, newManagersForUserWallet)
            }
        }
    }

    private suspend fun getWalletStores(userWallets: List<UserWallet>): List<WalletStoreModel> {
        return userWallets
            .map { it.walletId }
            .flatMap { userWalletId ->
                walletStoresStorage.getAll()
                    .firstOrNull()
                    ?.get(userWalletId)
                    .orEmpty()
            }
    }
}
