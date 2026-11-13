package com.tangem.data.collectibles.vault

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.data.collectibles.vault.transaction.DevnetGachaVaultTransactionSender
import com.tangem.data.collectibles.vault.transaction.GachaVaultSigners
import com.tangem.data.collectibles.vault.transaction.GachaVaultTransactionSender
import com.tangem.data.collectibles.vault.transaction.GaslessGachaVaultTransactionSender
import com.tangem.data.collectibles.vault.transaction.vaultWalletManager
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.delegate.CollectiblesSessionKey
import com.tangem.domain.collectibles.delegate.SessionKeyPublicKey
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.lib.crypto.BlockchainUtils.SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.Base64
import javax.inject.Inject

/**
 * Adapter over the blockchain SDK's `CollectiblesVaultProvider`, which `WalletManager` implements by delegation —
 * a manager of the wrong blockchain simply answers `isCollectiblesVaultSupported() == false`.
 *
 * Reads go straight to the provider. Writes go through a [GachaVaultTransactionSender]: on mainnet the gasless
 * sponsor pays, on devnet the wallet pays in SOL as before.
 */
@Suppress("LongParameterList")
internal class DefaultGachaVaultRepository @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
    private val networkProvider: CollectiblesOwnerNetworkProvider,
    private val currencyFactory: CollectiblesVaultCurrencyFactory,
    private val ownerDeriver: CollectiblesOwnerDeriver,
    private val signers: GachaVaultSigners,
    private val gaslessSender: GaslessGachaVaultTransactionSender,
    private val devnetSender: DevnetGachaVaultTransactionSender,
    private val sessionKey: CollectiblesSessionKey,
    private val dispatchers: CoroutineDispatcherProvider,
) : GachaVaultRepository {

    override suspend fun initOrUpdateVault(
        userWallet: UserWallet,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit> = withContext(dispatchers.io) {
        transactionSender(userWallet).initOrUpdateVault(userWallet, sessionKey, limits)
    }

    /**
     * The two shapes the derivation takes. A card holds the owner key, so deriving it means a session, and the
     * read and the signing are folded into that same session to keep the whole thing at one tap. A mobile wallet
     * derives from a seed it already has, so there is no session to fold anything into.
     */
    override suspend fun deriveOwnerAndSetUpVault(
        userWallet: UserWallet,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit> = withContext(dispatchers.io) {
        when (userWallet) {
            is UserWallet.Cold -> {
                TangemLogger.d("vault: deriveOwnerAndSetUpVault -> Cold wallet, deriving in a card session (tap)")
                transactionSender(userWallet).deriveOwnerAndSetUpVault(userWallet, sessionKey, limits)
            }
            is UserWallet.Hot -> {
                TangemLogger.d("vault: deriveOwnerAndSetUpVault -> Hot wallet, deriving locally (no tap)")
                either {
                    ownerDeriver.deriveOwner(userWallet.walletId).bind()
                    transactionSender(userWallet).initOrUpdateVault(userWallet, sessionKey, limits).bind()
                }
            }
        }
    }

    override suspend fun getVaultBalance(
        userWallet: UserWallet,
        cached: CollectiblesVaultBalance?,
    ): Either<CollectiblesAccountError, CollectiblesVaultBalance> = withContext(dispatchers.io) {
        either {
            val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()
            val walletManager = walletManagersFacade.vaultWalletManager(
                userWalletId = userWallet.walletId,
                network = network,
            )
                .mapLeft { it.toVaultFailure(userWallet) }
                .bind()

            val vaultAddress = walletManager.getCollectiblesVaultAddress().toEither()
                .mapLeft { it.toVaultFailure(userWallet) }
                .bind()

            val (token, ata) = cached?.let { it.usdcToken to it.ataAddress }
                ?: walletManager.getCollectiblesVaultUsdc().toEither()
                    .map { usdc -> currencyFactory.create(mint = usdc.mint) to usdc.address }
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
                ataAddress = ata,
            )
        }
    }

    override suspend fun getVaultSessionKey(userWallet: UserWallet): Either<CollectiblesAccountError, String?> =
        withContext(dispatchers.io) {
            either {
                val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()
                val walletManager = walletManagersFacade.vaultWalletManager(
                    userWalletId = userWallet.walletId,
                    network = network,
                )
                    .mapLeft { it.toVaultFailure(userWallet) }
                    .bind()

                walletManager.getCollectiblesVault().toEither()
                    .map { vault -> vault?.sessionKey }
                    .mapLeft { it.toVaultFailure(userWallet) }
                    .bind()
            }
        }

    override suspend fun getVaultAddress(userWallet: UserWallet): Either<CollectiblesAccountError, String> =
        withContext(dispatchers.io) {
            either {
                val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()

                walletManagersFacade.vaultWalletManager(userWalletId = userWallet.walletId, network = network)
                    .flatMap { walletManager -> walletManager.getCollectiblesVaultAddress().toEither() }
                    .mapLeft { error -> error.toVaultFailure(userWallet) }
                    .bind()
            }
        }

    override suspend fun sendPurchasePack(
        userWallet: UserWallet,
        amount: Long,
        feeAmount: Long,
        memo: String,
    ): Either<CollectiblesAccountError, String> = withContext(dispatchers.io) {
        either {
            val accountId = AccountId.forCollectiblesAccount(userWalletId = userWallet.walletId)
            val sessionKey = sessionKey.getPublicKeyOrNull(accountId)
                ?: raise(CollectiblesAccountError.SessionKeyNotProvisioned)

            var signingFailure: CollectiblesAccountError? = null
            val signer: suspend (ByteArray) -> Result<ByteArray> = { message ->
                this@DefaultGachaVaultRepository.sessionKey.sign(accountId, message).fold(
                    ifLeft = { error ->
                        signingFailure = error
                        Result.Failure(BlockchainSdkError.CustomError("Session key failed to sign: $error"))
                    },
                    ifRight = { signature -> Result.Success(signature) },
                )
            }

            transactionSender(userWallet).sendPurchasePack(
                userWallet = userWallet,
                sessionKey = sessionKey,
                amount = amount,
                feeAmount = feeAmount,
                memo = memo,
                signer = signer,
            )
                .mapLeft { error -> signingFailure ?: error }
                .bind()
        }
    }

    override suspend fun sendWithOwnerSignature(
        userWallet: UserWallet,
        transaction: String,
    ): Either<CollectiblesAccountError, String> = withContext(dispatchers.io) {
        either {
            val bytes = Either.catch { Base64.getDecoder().decode(transaction) }
                .mapLeft { GachaVaultError.Unexpected(it).asFailure() }
                .bind()

            // TODO Update after new firmware [REDACTED_JIRA]
            //  Pending: how to apply the check to older cards only, Tangem 3.0 cards don't need it
            if (userWallet is UserWallet.Cold) ensureCardCanSign(bytes).bind()

            val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()
            val walletManager = walletManagersFacade.vaultWalletManager(
                userWalletId = userWallet.walletId,
                network = network,
            )
                .mapLeft { error -> error.toVaultFailure(userWallet) }
                .bind()

            val transactionData = TransactionData.Compiled(value = TransactionData.Compiled.Data.Bytes(bytes))

            walletManager.send(transactionData, signers.signer(userWallet))
                .toEither()
                .map { result -> result.hash }
                .onLeft { error ->
                    TangemLogger.e("GachaVaultRepository: owner transaction failed: ${error.customMessage}")
                }
                .mapLeft { error -> error.toVaultFailure(userWallet) }
                .bind()
        }
    }

    /** A card signs at most this much message, so a larger one is refused before the tap. */
    private fun ensureCardCanSign(transaction: ByteArray): Either<CollectiblesAccountError, Unit> = either {
        val message = Either.catch { SolanaTransactionHelper.extractMessage(transaction) }
            .mapLeft { GachaVaultError.Unexpected(it).asFailure() }
            .bind()

        ensure(message.size <= SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES) {
            TangemLogger.e("GachaVaultRepository: owner transaction message is ${message.size} B, too large for a card")
            GachaVaultError.TransactionTooLarge.asFailure()
        }
    }

    private fun transactionSender(userWallet: UserWallet): GachaVaultTransactionSender =
        if (userWallet.collectiblesVaultBlockchain == Blockchain.SolanaTestnet) devnetSender else gaslessSender
}