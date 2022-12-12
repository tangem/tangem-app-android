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
import com.tangem.common.doOnSuccess
import com.tangem.common.flatMap
import com.tangem.common.flatMapOnFailure
import com.tangem.common.map
import com.tangem.common.services.Result
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.replaceByOrAdd
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.walletStores.WalletStoresError
import com.tangem.tap.domain.walletStores.implementation.utils.fold
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.implementation.utils.replaceWalletStore
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithAmounts
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithError
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithFiatRates
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithMissedDerivation
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithRent
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithUnreachable
import com.tangem.tap.domain.walletStores.storage.WalletManagerStorage
import com.tangem.tap.domain.walletStores.storage.WalletStoresStorage
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.tap.features.wallet.models.filterByCoin
import com.tangem.tap.features.wallet.models.getPendingTransactions
import com.tangem.tap.network.NetworkConnectivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal

internal class DefaultWalletAmountsRepository(
    private val tangemTechService: TangemTechService,
) : WalletAmountsRepository {
    private val walletStoresStorage = WalletStoresStorage
    private val walletManagersStorage = WalletManagerStorage

    override suspend fun update(
        userWallets: List<UserWallet>,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit> {
        return if (userWallets.isEmpty()) CompletionResult.Success(Unit)
        else withContext(Dispatchers.Default) {
            awaitAll(
                async { fetchAmounts(userWallets) },
                async { fetchFiatRates(userWallets, fiatCurrency) },
            )
                .fold()
        }
    }

    override suspend fun update(
        userWallet: UserWallet,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit> {
        return update(listOf(userWallet), fiatCurrency)
    }

    override suspend fun update(
        userWallet: UserWallet,
        walletStore: WalletStoreModel,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit> = withContext(Dispatchers.Default) {
        val walletId = userWallet.walletId
        val scanResponse = userWallet.scanResponse

        awaitAll(
            async {
// [REDACTED_TODO_COMMENT]
                val walletManager = walletStore.walletManager
                fetchAmounts(walletId, scanResponse, walletStore, walletManager)
                    .flatMap { fetchRentIfNeeded(walletStore, walletManager) }
            },
            async { fetchFiatRates(listOf(userWallet), fiatCurrency) },
        )
            .fold()
    }

    private suspend fun fetchAmounts(
        userWallets: List<UserWallet>,
    ): CompletionResult<Unit> = coroutineScope {
        userWallets.map { userWallet ->
            val walletId = userWallet.walletId
            val scanResponse = userWallet.scanResponse
            val walletStores = walletStoresStorage.getSync(walletId)

            walletStores.map { walletStore ->
                async {
// [REDACTED_TODO_COMMENT]
                    val walletManager = walletStore.walletManager
                    fetchAmounts(walletId, scanResponse, walletStore, walletManager)
                        .flatMap { fetchRentIfNeeded(walletStore, walletManager) }
                }
            }
                .awaitAll()
                .fold()
        }
            .fold()
    }

    private suspend fun fetchFiatRates(
        userWallets: List<UserWallet>,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit> {
        val walletsIds = userWallets.map { it.walletId }
        val walletStores = walletsIds
            .flatMap { walletStoresStorage.getSync(it) }

        val currencies = walletStores
            .asSequence()
            .flatMap { it.walletsData }
            .map { it.currency }

        val coinsIds = currencies.mapNotNull { it.coinId }.distinct().toList()

        val fiatRatesResult = withContext(Dispatchers.IO) {
            tangemTechService.rates(
                currency = fiatCurrency.code,
                ids = coinsIds,
            )
        }

        return when (fiatRatesResult) {
            is Result.Success -> {
                Timber.d(
                    """
                        Fetched fiat rates
                        |- User wallets ids: $walletsIds
                        |- Coins ids: $coinsIds
                    """.trimIndent(),
                )

                walletStores.forEach { walletStore ->
                    updateWithFiatRates(
                        walletStore = walletStore,
                        fiatRates = fiatRatesResult.data.rates,
                    )
                }

                CompletionResult.Success(Unit)
            }
            is Result.Failure -> {
                val error = WalletStoresError.FetchFiatRatesError(
                    currencies = currencies.map { it.currencySymbol }.toList(),
                    cause = fiatRatesResult.error,
                )

                Timber.e(
                    error,
                    """
                        Unable to fetch fiat rates
                        |- User wallets ids: $walletsIds
                        |- Coins ids: $coinsIds
                    """.trimIndent(),
                )

                CompletionResult.Failure(error)
            }
        }
    }

    private suspend fun fetchAmounts(
        walletId: UserWalletId,
        scanResponse: ScanResponse,
        walletStore: WalletStoreModel,
        walletManager: WalletManager?,
    ): CompletionResult<Unit> {
        val hasMissedDerivations = with(walletStore.blockchainNetwork) {
            derivationPath != null && !scanResponse.hasDerivation(blockchain, derivationPath)
        }
        val blockchain = walletStore.blockchainNetwork.blockchain
        val tokens = walletStore.blockchainNetwork.tokens.map { it.name }

        return when {
            hasMissedDerivations -> {
                Timber.e(
                    """
                        Missed derivation
                        |- User wallet id: $walletId
                        |- Blockchain: $blockchain
                    """.trimIndent(),
                )

                updateWithMissedDerivation(
                    walletStore = walletStore,
                )

                CompletionResult.Success(Unit)
            }
            walletManager == null -> {
                Timber.e(
                    """
                        Wallet manager is null
                        |- User wallet id: $walletId
                        |- Blockchain: $blockchain
                    """.trimIndent(),
                )

                updateWithUnreachable(
                    walletStore = walletStore,
                )

                CompletionResult.Success(Unit)
            }
            else -> {
                withInternetConnection { walletManager.update() }
                    .map { updateWalletManagerWithAmounts(walletId, walletManager) }
                    .doOnSuccess {
                        Timber.d(
                            """
                                Fetched amounts
                                |- User wallet id: $walletId
                                |- Blockchain: $blockchain
                                |- Tokens: $tokens
                            """.trimIndent(),
                        )

                        updateWithAmounts(
                            walletStore = walletStore,
                            wallet = walletManager.wallet,
                        )
                    }
                    .flatMapOnFailure { error ->
                        Timber.e(
                            error,
                            """
                                Unable to fetch amounts
                                |- User wallet id: $walletId
                                |- Blockchain: $blockchain
                                |- Tokens: $tokens
                            """.trimIndent(),
                        )

                        if (error is BlockchainSdkError) {
                            updateWithError(
                                walletStore = walletStore,
                                wallet = walletManager.wallet,
                                error = error,
                            )
                            CompletionResult.Success(Unit)
                        } else {
                            CompletionResult.Failure(error)
                        }
                    }
            }
        }
    }

    private suspend fun fetchRentIfNeeded(
        walletStore: WalletStoreModel,
        walletManager: WalletManager?,
    ): CompletionResult<Unit> {
        val rentProvider = walletManager as? RentProvider

        if (walletManager == null || rentProvider == null) {
            return CompletionResult.Success(Unit)
        }

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

                updateWithRent(
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

    private suspend inline fun withInternetConnection(crossinline block: suspend () -> Unit): CompletionResult<Unit> {
        return if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
            val error = WalletStoresError.NoInternetConnection
            Timber.e(error)
            CompletionResult.Failure(error)
        } else withContext(Dispatchers.IO) {
            catching { block() }
        }
    }

    private suspend fun updateWalletManagerWithAmounts(
        walletId: UserWalletId,
        walletManager: WalletManager,
    ) = withContext(Dispatchers.Default) {
        walletManagersStorage.update { prevManagers ->
            val newManagersForUserWallet = prevManagers[walletId].orEmpty()
                .toMutableList()
                .apply {
                    replaceByOrAdd(walletManager) {
                        it.wallet.blockchain == it.wallet.blockchain
                    }
                }

            prevManagers.apply {
                set(walletId, newManagersForUserWallet)
            }
        }
    }

    private suspend fun updateWithError(
        walletStore: WalletStoreModel,
        wallet: Wallet,
        error: BlockchainSdkError,
    ) = withContext(Dispatchers.Default) {
        walletStoresStorage.update { prevState ->
            prevState.replaceWalletStore(
                walletId = walletStore.userWalletId,
                walletStore = walletStore,
                update = {
                    it.updateWithError(
                        wallet = wallet,
                        error = error,
                    )
                },
            )
        }
    }

    private suspend fun updateWithAmounts(
        walletStore: WalletStoreModel,
        wallet: Wallet,
    ) = withContext(Dispatchers.Default) {
        walletStoresStorage.update { prevState ->
            prevState.replaceWalletStore(
                walletId = walletStore.userWalletId,
                walletStore = walletStore,
                update = {
                    it.updateWithAmounts(wallet = wallet)
                },
            )
        }
    }

    private suspend fun updateWithMissedDerivation(
        walletStore: WalletStoreModel,
    ) = withContext(Dispatchers.Default) {
        walletStoresStorage.update { prevState ->
            prevState.replaceWalletStore(
                walletId = walletStore.userWalletId,
                walletStore = walletStore,
                update = {
                    it.updateWithMissedDerivation()
                },
            )
        }
    }

    private suspend fun updateWithUnreachable(
        walletStore: WalletStoreModel,
    ) = withContext(Dispatchers.Default) {
        walletStoresStorage.update { prevState ->
            prevState.replaceWalletStore(
                walletId = walletStore.userWalletId,
                walletStore = walletStore,
                update = {
                    it.updateWithUnreachable()
                },
            )
        }
    }

    private suspend fun updateWithFiatRates(
        walletStore: WalletStoreModel,
        fiatRates: Map<String, Double>,
    ) = withContext(Dispatchers.Default) {
        walletStoresStorage.update { prevState ->
            prevState.replaceWalletStore(
                walletId = walletStore.userWalletId,
                walletStore = walletStore,
                update = {
                    it.updateWithFiatRates(rates = fiatRates)
                },
            )
        }
    }

    private suspend fun updateWithRent(
        walletStore: WalletStoreModel,
        rent: WalletStoreModel.WalletRent?,
    ) = withContext(Dispatchers.Default) {
        walletStoresStorage.update { prevState ->
            prevState.replaceWalletStore(
                walletId = walletStore.userWalletId,
                walletStore = walletStore,
                update = {
                    it.updateWithRent(rent)
                },
            )
        }
    }
}
