package com.tangem.data.collectibles.vault.transaction

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
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
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * The wallet pays: the owner covers rent and network fee in SOL, the session key covers the spin fee in SOL and is
 * topped up by hand. Signing and broadcasting are one `send`, so the card session is released from inside it.
 */
// TODO Collectibles devnet: delete everything marked so once the prod CC is set up for QA
internal class DevnetGachaVaultTransactionSender @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
    private val networkProvider: CollectiblesOwnerNetworkProvider,
    private val derivationsRepository: DerivationsRepository,
    private val tangemSdkManager: TangemSdkManager,
    private val signers: GachaVaultSigners,
) : GachaVaultTransactionSender {

    override suspend fun initOrUpdateVault(
        userWallet: UserWallet,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit> = either {
        val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()

        initOrUpdateVaultInternal(
            userWalletId = userWallet.walletId,
            network = network,
            sessionKey = sessionKey,
            limits = limits,
            signer = signers.signer(userWallet),
            onSigned = {},
        )
            .onLeft { error ->
                TangemLogger.e("GachaVaultRepository: initOrUpdateVault failed: ${error.customMessage}")
            }
            .mapLeft { error -> error.toVaultFailure(userWallet) }
            .bind()
    }

    override suspend fun deriveOwnerAndSetUpVault(
        userWallet: UserWallet.Cold,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit> = coroutineScope {
        val seedKey = userWallet.solanaSeedKey()

        if (seedKey == null) {
            TangemLogger.d("vault: the wallet has no Solana key -> WalletUnavailable")
            return@coroutineScope GachaVaultError.WalletUnavailable.asFailure().left()
        }

        val vaultSetUpCompletion = CompletableDeferred<CompletionResult<Unit>>()
        val cardId = userWallet.sessionCardId()
        val task = DeriveCollectiblesOwnerAndSetUpVaultTask<Unit>(
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
                        sessionKey = sessionKey,
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

    override suspend fun sendPurchasePack(
        userWallet: UserWallet,
        sessionKey: SessionKeyPublicKey,
        amount: Long,
        feeAmount: Long,
        memo: String,
        signer: suspend (message: ByteArray) -> Result<ByteArray>,
    ): Either<CollectiblesAccountError, String> = either {
        val network = networkProvider.create(userWallet).mapLeft { it.asFailure() }.bind()
        val walletManager = walletManagersFacade.vaultWalletManager(
            userWalletId = userWallet.walletId,
            network = network,
        )
            .mapLeft { error -> error.toVaultFailure(userWallet) }
            .bind()

        walletManager.sendPurchasePack(
            sessionKey = sessionKey.base58,
            amount = amount,
            feeAmount = feeAmount,
            memo = memo,
            signer = signer,
        )
            .toEither()
            .map { result -> result.hash }
            .onLeft { error -> TangemLogger.e("GachaVaultRepository: purchase_pack failed: ${error.customMessage}") }
            .mapLeft { error -> error.toVaultFailure(userWallet) }
            .bind()
    }

    private fun CompletionResult<Unit>.toVaultResult(): Either<CollectiblesAccountError, Unit> = when (this) {
        is CompletionResult.Success -> Unit.right()
        is CompletionResult.Failure -> error.toVaultFailure().left()
    }

    @Suppress("LongParameterList")
    private suspend fun setUpVault(
        userWallet: UserWallet.Cold,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
        session: CardSession,
        onSigned: () -> Unit,
    ): CompletionResult<Unit> = either {
        val network = networkProvider.create(userWallet).mapLeft { it.tangemError }.bind()

        initOrUpdateVaultInternal(
            userWalletId = userWallet.walletId,
            network = network,
            sessionKey = sessionKey,
            limits = limits,
            signer = signers.signer(userWallet = userWallet, session = session),
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

    @Suppress("LongParameterList")
    private suspend fun initOrUpdateVaultInternal(
        userWalletId: UserWalletId,
        network: Network,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
        signer: TransactionSigner,
        onSigned: () -> Unit,
    ): Either<BlockchainError, Unit> = either {
        val walletManager = walletManagersFacade.vaultWalletManager(userWalletId = userWalletId, network = network)
            .bind()
        val transaction = walletManager.buildInitOrUpdateVault(sessionKey = sessionKey, limits = limits).bind()
            ?: return@either

        walletManager.send(transaction, signer, onSigned).toEither()
            .onLeft { TangemLogger.d("vault: set-up transaction failed: ${it.customMessage}") }
            .onRight { TangemLogger.d("vault: set-up transaction sent") }
            .bind()
    }
}