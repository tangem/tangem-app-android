package com.tangem.data.yield.supply.status

import arrow.core.Either
import arrow.core.flatten
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.data.yield.supply.status.store.YieldModuleStatusStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.status.YieldModuleStatusFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultYieldModuleStatusFetcher @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
    private val store: YieldModuleStatusStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldModuleStatusFetcher {

    override suspend fun invoke(params: YieldModuleStatusFetcher.Params): Either<Throwable, Unit> {
        if (params.tokens.isEmpty()) return Unit.right()

        TangemLogger.i("Start fetching Yield Module statuses for params:\n$params")

        val failures = coroutineScope {
            params.tokens
                .groupBy(CryptoCurrency.Token::network)
                .map { (network, tokens) ->
                    async { fetchNetwork(userWalletId = params.userWalletId, network = network, tokens = tokens) }
                }
                .awaitAll()
                .mapNotNull { it.leftOrNull() }
        }

        return failures.firstOrNull()?.left() ?: Unit.right()
    }

    private suspend fun fetchNetwork(
        userWalletId: UserWalletId,
        network: Network,
        tokens: List<CryptoCurrency.Token>,
    ): Either<Throwable, Unit> {
        val keys = tokens.mapTo(hashSetOf()) { YieldModuleStatus.Key(it) }

        return catchUnlessCancellation {
            withContext(dispatchers.io) {
                // A network without a derived key is MissedDerivation for the network status, not a failure: a Left
                // here would abort the whole wallet refresh
                val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
                if (walletManager == null) {
                    store.storeError(userWalletId = userWalletId, keys = keys)
                    return@withContext Unit.right()
                }

                if (!walletManager.isSupported()) return@withContext Unit.right()

                store.refresh(userWalletId = userWalletId, keys = keys)

                readStatuses(
                    userWalletId = userWalletId,
                    network = network,
                    walletManager = walletManager,
                    tokens = tokens,
                )
            }
        }
            .onLeft { throwable ->
                TangemLogger.e("Unable to fetch Yield Module statuses of ${network.id} for $userWalletId", throwable)
                store.storeError(userWalletId = userWalletId, keys = keys)
            }
            .flatten()
    }

    private suspend fun readStatuses(
        userWalletId: UserWalletId,
        network: Network,
        walletManager: WalletManager,
        tokens: List<CryptoCurrency.Token>,
    ): Either<Throwable, Unit> {
        if (walletManager.getYieldModuleAddress() == EthereumUtils.ZERO_ADDRESS) {
            // The SDK also answers ZERO_ADDRESS when the address RPC fails. The module is one per network, so an
            // active status of any token on it proves the module exists and the answer is a failure
            check(!hasActiveStatus(userWalletId = userWalletId, network = network)) {
                "Yield Module address is unavailable while an active status is stored"
            }

            store.storeActual(
                userWalletId = userWalletId,
                statuses = tokens.mapTo(hashSetOf(), ::createInactiveStatus),
            )
            return Unit.right()
        }

        val results = coroutineScope {
            tokens.map { token -> async { token to readStatus(walletManager = walletManager, token = token) } }
                .awaitAll()
        }

        val statuses = results.mapNotNullTo(hashSetOf()) { (_, status) -> status.getOrNull() }
        val failures = results.mapNotNull { (token, status) ->
            status.leftOrNull()?.let { throwable ->
                TangemLogger.e("Unable to read Yield Module status of ${token.id}", throwable)
                YieldModuleStatus.Key(token) to throwable
            }
        }

        store.storeActual(userWalletId = userWalletId, statuses = statuses)
        store.storeError(userWalletId = userWalletId, keys = failures.mapTo(hashSetOf()) { it.first })

        return failures.firstOrNull()?.second?.left() ?: Unit.right()
    }

    private suspend fun hasActiveStatus(userWalletId: UserWalletId, network: Network): Boolean {
        return store.getSync(userWalletId).any { it.isActive && it.networkId == network.id }
    }

    private suspend fun readStatus(
        walletManager: WalletManager,
        token: CryptoCurrency.Token,
    ): Either<Throwable, YieldModuleStatus> = catchUnlessCancellation {
        // The SDK answers a failed read with null, which must not be stored as "not supplied"
        val sdkStatus = walletManager.getYieldSupplyStatus(token.contractAddress)
            ?: error("Yield supply status is unavailable")

        val sdkToken = Token(symbol = token.symbol, contractAddress = token.contractAddress, decimals = token.decimals)
        val protocolBalance = if (sdkStatus.isActive) {
            walletManager.getEffectiveProtocolBalance(sdkToken)
        } else {
            null
        }

        createInactiveStatus(token).copy(
            isActive = sdkStatus.isActive,
            isInitialized = sdkStatus.isInitialized,
            isAllowedToSpend = walletManager.isAllowedToSpend(sdkToken),
            effectiveProtocolBalance = protocolBalance,
        )
    }

    private fun createInactiveStatus(token: CryptoCurrency.Token): YieldModuleStatus {
        val key = YieldModuleStatus.Key(token)

        return YieldModuleStatus(
            networkId = key.networkId,
            tokenAddress = key.tokenAddress,
            isActive = false,
            isInitialized = false,
            isAllowedToSpend = false,
            effectiveProtocolBalance = null,
            source = StatusSource.ACTUAL,
        )
    }

    // The SDK wraps a cancelled RPC into its own error with the cancellation as the cause. While this coroutine is
    // still active the cancellation is the SDK's own (e.g. a timeout), which is a failure
    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <T> catchUnlessCancellation(block: () -> T): Either<Throwable, T> {
        return try {
            block().right()
        } catch (throwable: Throwable) {
            val cancellation = throwable as? CancellationException ?: throwable.cause as? CancellationException
            if (cancellation != null && !currentCoroutineContext().isActive) throw cancellation

            throwable.left()
        }
    }
}