package com.tangem.data.collectibles.vault

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.core.raise.either
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.core.error.ext.tangemError
import com.tangem.data.collectibles.account.derivation.DeriveCollectiblesOwnerAndSetUpVaultTask
import com.tangem.data.collectibles.account.derivation.solanaSeedKey
import com.tangem.data.wallets.cold.UserWalletIdPreflightReadFilter
import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * Adapter over the blockchain SDK's `CollectiblesVaultProvider`, which `WalletManager` implements by delegation —
 * a manager of the wrong blockchain simply answers `isCollectiblesVaultSupported() == false`.
 *
 * The provider only builds wire bytes; the manager's own `send` signs and broadcasts them. The wallet is therefore
 * both `cold_owner` and `payer` — rent and network fee come out of its SOL balance.
 */
@Suppress("LongParameterList", "LargeClass")
internal class DefaultGachaVaultRepository @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
    private val networkProvider: CollectiblesOwnerNetworkProvider,
    private val currencyFactory: CollectiblesVaultCurrencyFactory,
    private val ownerDeriver: CollectiblesOwnerDeriver,
    private val derivationsRepository: DerivationsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val tangemSdkManager: TangemSdkManager,
    private val hotWalletSignerFactory: TangemHotWalletSigner.Factory,
    private val dispatchers: CoroutineDispatcherProvider,
) : GachaVaultRepository {

    override suspend fun initOrUpdateVault(
        userWallet: UserWallet,
        hotDelegate: HotDelegatePublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit> = withContext(dispatchers.io) {
        either {
            val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()

            initOrUpdateVaultInternal(
                userWalletId = userWallet.walletId,
                network = network,
                hotDelegate = hotDelegate,
                limits = limits,
                signer = userWallet.signer(),
                onSigned = {},
            )
                .onLeft { error ->
                    TangemLogger.e("GachaVaultRepository: initOrUpdateVault failed: ${error.customMessage}")
                }
                .mapLeft { error -> error.toVaultFailure(userWallet) }
                .bind()
        }
    }

    /**
     * The two shapes the derivation takes. A card holds the owner key, so deriving it means a session, and the
     * read and the signing are folded into that same session to keep the whole thing at one tap. A mobile wallet
     * derives from a seed it already has, so there is no session to fold anything into.
     */
    override suspend fun deriveOwnerAndSetUpVault(
        userWallet: UserWallet,
        hotDelegate: HotDelegatePublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit> = withContext(dispatchers.io) {
        when (userWallet) {
            is UserWallet.Cold -> {
                TangemLogger.d("vault: deriveOwnerAndSetUpVault -> Cold wallet, deriving in a card session (tap)")
                deriveAndInitInCardSession(userWallet, hotDelegate, limits)
            }
            is UserWallet.Hot -> {
                TangemLogger.d("vault: deriveOwnerAndSetUpVault -> Hot wallet, deriving locally (no tap)")
                deriveAndInitLocally(userWallet, hotDelegate, limits)
            }
        }
    }

    override suspend fun getVaultBalance(
        userWallet: UserWallet,
        cachedToken: CryptoCurrency.Token?,
    ): Either<CollectiblesAccountError, CollectiblesVaultBalance> = withContext(dispatchers.io) {
        either {
            val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()
            val walletManager = vaultWalletManager(userWalletId = userWallet.walletId, network = network)
                .mapLeft { it.toVaultFailure(userWallet) }
                .bind()

            val vaultAddress = walletManager.getCollectiblesVaultAddress().toEither()
                .mapLeft { it.toVaultFailure(userWallet) }
                .bind()

            val token = cachedToken ?: walletManager.getCollectiblesVaultUsdc().toEither()
                .map { usdc -> currencyFactory.create(mint = usdc.mint) }
                .mapLeft { it.toVaultFailure(userWallet) }
                .bind()

            // TODO find out the right way to get vault balance
            val balances = walletManagersFacade.getTokenBalances(
                userWalletId = userWallet.walletId,
                network = network,
                address = vaultAddress,
            )

            CollectiblesVaultBalance(
                usdcToken = token,
                // The provider lists only the accounts holding something, so an absent entry is an empty one
                usdcAmount = balances.firstOrNull { it.contractAddress == token.contractAddress }?.amount
                    ?: BigDecimal.ZERO,
            )
        }
    }

    override suspend fun getVaultTopUpAddress(userWallet: UserWallet): Either<CollectiblesAccountError, String> =
        withContext(dispatchers.io) {
            either {
                val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()

                vaultWalletManager(userWalletId = userWallet.walletId, network = network)
                    .flatMap { walletManager -> walletManager.getCollectiblesVaultUsdc().toEither() }
                    .map { usdc -> usdc.address }
                    .mapLeft { it.toVaultFailure(userWallet) }
                    .bind()
            }
        }

    override suspend fun closeVault(userWallet: UserWallet): Either<CollectiblesAccountError, Unit> =
        withContext(dispatchers.io) {
            either {
                val network = networkProvider.create(userWallet).mapLeft { error -> error.asFailure() }.bind()

                closeVaultInternal(
                    userWalletId = userWallet.walletId,
                    network = network,
                    signer = userWallet.signer(),
                )
                    .onLeft { error ->
                        TangemLogger.e("GachaVaultRepository: closeVault failed: ${error.customMessage}")
                    }
                    .mapLeft { error -> error.toVaultFailure(userWallet) }
                    .bind()
            }
        }

    private suspend fun deriveAndInitInCardSession(
        userWallet: UserWallet.Cold,
        hotDelegate: HotDelegatePublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit> = coroutineScope {
        val seedKey = userWallet.solanaSeedKey()

        if (seedKey == null) {
            TangemLogger.d("vault: the wallet has no Solana key -> WalletUnavailable")
            return@coroutineScope GachaVaultError.WalletUnavailable.asFailure().left()
        }

        val vaultSetUpCompletion = CompletableDeferred<CompletionResult<Unit>>()
        val cardId = userWallet.sessionCardId()
        val task = DeriveCollectiblesOwnerAndSetUpVaultTask(
            userWallet = userWallet,
            seedKey = seedKey,
            derivationsRepository = derivationsRepository,
            scope = this,
            setUpVault = { session, callback ->
                var isSigned = false

                // A throw here would take this scope down, the session's own await included
                val result = runSuspendCatching {
                    setUpVault(
                        userWallet = userWallet,
                        hotDelegate = hotDelegate,
                        limits = limits,
                        session = session,
                        onSigned = {
                            isSigned = true
                            callback(CompletionResult.Success(Unit))
                        },
                    )
                }.getOrElse { error ->
                    TangemLogger.e("GachaVaultRepository: the one-tap set-up threw", error)
                    CompletionResult.Failure(GachaVaultError.Unexpected(cause = error).tangemError)
                }

                if (!isSigned) callback(result)
                vaultSetUpCompletion.complete(result)
            },
        )

        val result = tangemSdkManager.runTaskAsync(
            runnable = task,
            preflightReadFilter = if (cardId == null) {
                UserWalletIdPreflightReadFilter(expectedUserWallet = userWallet)
            } else {
                null
            },
            cardId = cardId,
            registerColdWallet = false,
        )

        when (result) {
            is CompletionResult.Success -> vaultSetUpCompletion.await().toVaultResult()
            is CompletionResult.Failure -> result.error.toVaultFailure().left()
        }.also { TangemLogger.d("vault: one-tap session finished, result=$it") }
    }

    private fun CompletionResult<Unit>.toVaultResult(): Either<CollectiblesAccountError, Unit> = when (this) {
        is CompletionResult.Success -> Unit.right()
        is CompletionResult.Failure -> error.toVaultFailure().left()
    }

    @Suppress("LongParameterList")
    private suspend fun setUpVault(
        userWallet: UserWallet.Cold,
        hotDelegate: HotDelegatePublicKey,
        limits: GachaVaultLimits,
        session: CardSession,
        onSigned: () -> Unit,
    ): CompletionResult<Unit> = either {
        val network = networkProvider.create(userWallet).mapLeft { it.tangemError }.bind()

        initOrUpdateVaultInternal(
            userWalletId = userWallet.walletId,
            network = network,
            hotDelegate = hotDelegate,
            limits = limits,
            signer = userWallet.signer(session = session),
            onSigned = onSigned,
        )
            .onLeft { error ->
                TangemLogger.e(
                    messageString = "GachaVaultRepository: the one-tap set-up failed: " +
                        "${error::class.simpleName}(code=${error.code}) ${error.customMessage}",
                    throwable = (error as? BlockchainSdkError)?.cause,
                )
            }
            .mapLeft { error -> error.toTangemError() }
            .bind()
    }.fold(
        ifLeft = { error -> CompletionResult.Failure(error) },
        ifRight = { CompletionResult.Success(Unit) },
    )

    private suspend fun deriveAndInitLocally(
        userWallet: UserWallet.Hot,
        hotDelegate: HotDelegatePublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit> = either {
        ownerDeriver.deriveOwner(userWallet.walletId).bind()
        initOrUpdateVault(userWallet, hotDelegate, limits).bind()
    }

    private suspend fun closeVaultInternal(
        userWalletId: UserWalletId,
        network: Network,
        signer: TransactionSigner,
    ): Either<BlockchainError, Unit> = either {
        vaultWalletManager(userWalletId = userWalletId, network = network)
            .bind()
            .sendVaultTransaction(signer) { buildCloseCollectiblesVault() }
            .bind()
    }

    /** Reading first keeps it at one signature: `init_vault` over an existing vault fails with `already in use`. */
    @Suppress("LongParameterList")
    private suspend fun initOrUpdateVaultInternal(
        userWalletId: UserWalletId,
        network: Network,
        hotDelegate: HotDelegatePublicKey,
        limits: GachaVaultLimits,
        signer: TransactionSigner,
        onSigned: () -> Unit,
    ): Either<BlockchainError, Unit> = either {
        val walletManager = vaultWalletManager(userWalletId = userWalletId, network = network).bind()
        val existing = walletManager.getCollectiblesVault().toEither()
            .onLeft { TangemLogger.d("vault: getCollectiblesVault failed: ${it.customMessage}") }
            .bind()

        when {
            existing == null -> {
                TangemLogger.d(
                    "vault: does the User Account PDA exist on chain? -> " +
                        "No, creating the User Account PDA (init_vault) (tap)",
                )
                walletManager.sendVaultTransaction(signer, onSigned) {
                    buildInitCollectiblesVault(
                        hotDelegate = hotDelegate.base58,
                        perSpinCap = limits.perSpinCap,
                        dailyCap = limits.dailyCap,
                    )
                }
                    .onLeft { TangemLogger.d("vault: init_vault failed: ${it.customMessage}") }
                    .onRight { TangemLogger.d("vault: init_vault sent") }
                    .bind()
            }

            existing.hotDelegate != hotDelegate.base58 -> {
                TangemLogger.d(
                    "vault: is the hot key the same as in the User Account PDA? -> " +
                        "No, updating the key in the PDA (update_vault) (tap)",
                )
                walletManager.sendVaultTransaction(signer, onSigned) {
                    buildUpdateCollectiblesVault(
                        hotDelegate = hotDelegate.base58,
                        perSpinCap = null,
                        dailyCap = null,
                    )
                }
                    .onLeft { TangemLogger.d("vault: update_vault failed: ${it.customMessage}") }
                    .onRight { TangemLogger.d("vault: update_vault sent") }
                    .bind()
            }

            else -> TangemLogger.d(
                "vault: is the hot key the same as in the User Account PDA? -> Yes, no transaction needed",
            )
        }
    }

    /** Built from the wallet's stored derivations, so the owner key has to be persisted before this is called. */
    @Suppress("DEPRECATION")
    private suspend fun vaultWalletManager(
        userWalletId: UserWalletId,
        network: Network,
    ): Either<BlockchainError, WalletManager> {
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        ) ?: return BlockchainSdkError.CustomError(
            "Failed to create or retrieve wallet manager: userWalletId=$userWalletId, network=${network.id}",
        ).left()

        return walletManager.right()
    }

    /** The SDK builds the wire bytes and the manager broadcasts them, so [TransactionData] never leaves here. */
    private suspend fun WalletManager.sendVaultTransaction(
        signer: TransactionSigner,
        onSigned: () -> Unit = {},
        build: suspend WalletManager.() -> Result<TransactionData.Compiled>,
    ): Either<BlockchainError, Unit> = either {
        val transactionData = build().toEither().bind()
        send(transactionData, signer, onSigned).toEither().bind()
    }

    private fun UserWallet.signer(): TransactionSigner = when (this) {
        is UserWallet.Cold -> cardSdkConfigRepository.getCommonSigner(
            cardId = sessionCardId(),
            twinKey = TwinKey.getOrNull(scanResponse = scanResponse),
            userWalletId = walletId,
        )
        is UserWallet.Hot -> hotWalletSignerFactory.create(this)
    }

    private fun UserWallet.Cold.signer(session: CardSession): TransactionSigner =
        cardSdkConfigRepository.getCommonSigner(
            cardId = sessionCardId(),
            twinKey = TwinKey.getOrNull(scanResponse = scanResponse),
            userWalletId = walletId,
            session = session,
        )

    /** A card that is not backed up must answer with that very card, so the session is pinned to its id. */
    private fun UserWallet.Cold.sessionCardId(): String? {
        val card = scanResponse.card
        val isBackedUp = card.backupStatus?.isActive == true

        return card.cardId.takeIf { !isBackedUp && !card.isTangemTwins }
    }
}