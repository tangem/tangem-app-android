package com.tangem.data.collectibles.vault.transaction

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.right
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessSignedTransaction
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessTransactionSigner
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.core.error.ext.tangemError
import com.tangem.data.collectibles.account.derivation.DeriveCollectiblesOwnerAndSetUpVaultTask
import com.tangem.data.collectibles.account.derivation.solanaSeedKey
import com.tangem.data.collectibles.vault.CollectiblesOwnerNetworkProvider
import com.tangem.data.collectibles.vault.asFailure
import com.tangem.data.collectibles.vault.sessionCardId
import com.tangem.data.collectibles.vault.toEither
import com.tangem.data.collectibles.vault.toTangemError
import com.tangem.data.collectibles.vault.toVaultFailure
import com.tangem.data.wallets.cold.UserWalletIdPreflightReadFilter
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.delegate.SessionKeyPublicKey
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.SolanaGaslessTransactionRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * The gasless sponsor pays the fee and the vault's rent. The wallet only signs its own slot; the gasless service
 * co-signs and broadcasts. Nothing is broadcast from the card session, so it simply returns the signed bytes.
 */
@Suppress("LongParameterList")
internal class GaslessGachaVaultTransactionSender @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
    private val networkProvider: CollectiblesOwnerNetworkProvider,
    private val derivationsRepository: DerivationsRepository,
    private val tangemSdkManager: TangemSdkManager,
    private val signers: GachaVaultSigners,
    private val gaslessRepository: SolanaGaslessTransactionRepository,
) : GachaVaultTransactionSender {

    override suspend fun initOrUpdateVault(
        userWallet: UserWallet,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit> = either {
        val feePayer = feePayer()
        val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()

        val signed = signVaultSetUp(
            userWalletId = userWallet.walletId,
            network = network,
            sessionKey = sessionKey,
            limits = limits,
            feePayer = feePayer,
            signer = signers.signer(userWallet),
        )
            .onLeft { error ->
                TangemLogger.e("GachaVaultRepository: initOrUpdateVault failed: ${error.customMessage}")
            }
            .mapLeft { failure -> failure.toVaultFailure(userWallet) }
            .bind()
            ?: return@either

        submit(userWalletId = userWallet.walletId, signed = signed)
        TangemLogger.d("vault: set-up transaction sent")
    }

    override suspend fun deriveOwnerAndSetUpVault(
        userWallet: UserWallet.Cold,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit> = either {
        val seedKey = userWallet.solanaSeedKey() ?: raise(GachaVaultError.WalletUnavailable.asFailure())
        val feePayer = feePayer()

        val signed = signInCardSession(
            userWallet = userWallet,
            seedKey = seedKey,
            sessionKey = sessionKey,
            limits = limits,
            feePayer = feePayer,
        ).bind() ?: return@either

        submit(userWalletId = userWallet.walletId, signed = signed)
        TangemLogger.d("vault: set-up transaction sent")
    }

    override suspend fun sendPurchasePack(
        userWallet: UserWallet,
        sessionKey: SessionKeyPublicKey,
        amount: Long,
        feeAmount: Long,
        memo: String,
        signer: suspend (message: ByteArray) -> Result<ByteArray>,
    ): Either<CollectiblesAccountError, String> = either {
        val feePayer = feePayer()
        val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()
        val walletManager = walletManagersFacade.vaultWalletManager(
            userWalletId = userWallet.walletId,
            network = network,
        )
            .mapLeft { error -> error.toVaultFailure(userWallet) }
            .bind()

        val signed = walletManager.signGaslessPurchasePack(
            sessionKey = sessionKey.base58,
            amount = amount,
            feeAmount = feeAmount,
            memo = memo,
            feePayer = feePayer,
            signer = signer,
        )
            .toEither()
            .onLeft { error -> TangemLogger.e("GachaVaultRepository: purchase_pack failed: ${error.customMessage}") }
            .mapLeft { error -> error.toVaultFailure(userWallet) }
            .bind()

        submit(userWalletId = userWallet.walletId, signed = signed)
    }

    override suspend fun withdraw(
        userWallet: UserWallet,
        mint: String,
        recipient: String,
        amount: Long,
    ): Either<CollectiblesAccountError, String> = either {
        val feePayer = feePayer()
        val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()

        val signed = signWithdraw(
            userWalletId = userWallet.walletId,
            network = network,
            mint = mint,
            recipient = recipient,
            amount = amount,
            feePayer = feePayer,
            signer = signers.signer(userWallet),
        )
            .onLeft { error -> TangemLogger.e("GachaVaultRepository: withdraw_token failed: ${error.customMessage}") }
            .mapLeft { error -> error.toVaultFailure(userWallet) }
            .bind()

        submit(userWalletId = userWallet.walletId, signed = signed)
    }

    private suspend fun signInCardSession(
        userWallet: UserWallet.Cold,
        seedKey: ByteArray,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
        feePayer: String,
    ): Either<CollectiblesAccountError, SolanaGaslessSignedTransaction?> = coroutineScope {
        val cardId = userWallet.sessionCardId()
        val task = DeriveCollectiblesOwnerAndSetUpVaultTask(
            userWallet = userWallet,
            seedKey = seedKey,
            derivationsRepository = derivationsRepository,
            scope = this,
            setUpVault = { session, callback ->
                // A throw here would take this scope down, the session's own await included
                val result = runSuspendCatching {
                    signInSession(
                        userWallet = userWallet,
                        sessionKey = sessionKey,
                        limits = limits,
                        feePayer = feePayer,
                        session = session,
                    )
                }.getOrElse { error ->
                    TangemLogger.e("GachaVaultRepository: the one-tap set-up threw", error)
                    CompletionResult.Failure(GachaVaultError.Unexpected(cause = error).tangemError)
                }

                callback(result)
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
            is CompletionResult.Success -> result.data.right()
            is CompletionResult.Failure -> result.error.toVaultFailure().left()
        }
    }

    private suspend fun signInSession(
        userWallet: UserWallet.Cold,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
        feePayer: String,
        session: CardSession,
    ): CompletionResult<SolanaGaslessSignedTransaction?> = either {
        val network = networkProvider.create(userWallet).mapLeft { it.tangemError }.bind()

        signVaultSetUp(
            userWalletId = userWallet.walletId,
            network = network,
            sessionKey = sessionKey,
            limits = limits,
            feePayer = feePayer,
            signer = signers.signer(userWallet = userWallet, session = session),
        )
            .onLeft { failure -> TangemLogger.e("GachaVaultRepository: the one-tap set-up failed: $failure") }
            .mapLeft { failure -> failure.toTangemError() }
            .bind()
    }.fold(
        ifLeft = { error -> CompletionResult.Failure(error) },
        ifRight = { signed -> CompletionResult.Success(signed) },
    )

    @Suppress("LongParameterList")
    private suspend fun signVaultSetUp(
        userWalletId: UserWalletId,
        network: Network,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
        feePayer: String,
        signer: TransactionSigner,
    ): Either<BlockchainError, SolanaGaslessSignedTransaction?> = either {
        val walletManager = walletManagersFacade.vaultWalletManager(userWalletId = userWalletId, network = network)
            .bind()
        val gaslessSigner = walletManager as? SolanaGaslessTransactionSigner
            ?: raise(BlockchainSdkError.UnsupportedOperation("No Solana gasless signer"))
        val transaction = walletManager.buildInitOrUpdateVault(sessionKey = sessionKey, limits = limits).bind()
            ?: return@either null

        gaslessSigner.signGaslessTransaction(
            transactionData = transaction,
            feePayer = feePayer,
            signer = signer,
            instructionRules = walletManager.getGaslessInstructionRules(),
        )
            .toEither()
            .bind()
    }

    @Suppress("LongParameterList")
    private suspend fun signWithdraw(
        userWalletId: UserWalletId,
        network: Network,
        mint: String,
        recipient: String,
        amount: Long,
        feePayer: String,
        signer: TransactionSigner,
    ): Either<BlockchainError, SolanaGaslessSignedTransaction> = either {
        val walletManager = walletManagersFacade.vaultWalletManager(userWalletId = userWalletId, network = network)
            .bind()
        val gaslessSigner = walletManager as? SolanaGaslessTransactionSigner
            ?: raise(BlockchainSdkError.UnsupportedOperation("No Solana gasless signer"))
        val transaction = walletManager.buildWithdrawCollectiblesVault(
            mint = mint,
            recipient = recipient,
            amount = amount,
        )
            .toEither()
            .bind()

        gaslessSigner.signGaslessTransaction(
            transactionData = transaction,
            feePayer = feePayer,
            signer = signer,
            instructionRules = walletManager.getGaslessInstructionRules(),
        )
            .toEither()
            .bind()
    }

    private suspend fun Raise<CollectiblesAccountError>.feePayer(): String = gaslessRepository.getFeePayer()
        .onLeft { error -> TangemLogger.e("GachaVaultRepository: no gasless fee payer: $error") }
        .mapLeft { error -> error.toVaultFailure() }
        .bind()

    private suspend fun Raise<CollectiblesAccountError>.submit(
        userWalletId: UserWalletId,
        signed: SolanaGaslessSignedTransaction,
    ): String = gaslessRepository.submit(
        userWalletId = userWalletId,
        transaction = signed.transaction,
        lastValidBlockHeight = signed.lastValidBlockHeight,
    )
        .onLeft { error -> TangemLogger.e("GachaVaultRepository: gasless submit failed: $error") }
        .mapLeft { error -> error.toVaultFailure() }
        .bind()
}