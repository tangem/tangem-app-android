package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.models.BuybackError
import com.tangem.domain.collectibles.models.CollectibleBuybackStatus
import com.tangem.domain.collectibles.models.CollectibleBuybackTx
import com.tangem.domain.collectibles.models.GenerateBuybackTxParams
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Sells a won card back: the backend builds the transaction, the owner signs it as is, the backend watches it land.
 *
 * Only a right means the card is gone; a retry after any failure is safe, as the card can leave the owner only once.
 */
class BuybackCollectibleUseCase @Inject constructor(
    private val collectiblesRepository: CollectiblesRepository,
    private val gachaVaultRepository: GachaVaultRepository,
    private val ownerDeriver: CollectiblesOwnerDeriver,
    private val accountStatusFetcher: CollectiblesAccountStatusFetcher,
    private val appScope: AppCoroutineScope,
) {

    suspend operator fun invoke(userWallet: UserWallet, nftAddress: String): Either<BuybackError, Unit> {
        val result = either {
            val tx = generateTx(userWallet, nftAddress)
            val txHash = gachaVaultRepository.sendWithOwnerSignature(userWallet, tx.transaction)
                .getOrElse { error -> raise(error.toBuybackError()) }

            reportTxHash(userWallet, tx.buybackId, txHash)
            awaitConfirmation(userWallet, tx.buybackId)
        }.onLeft { error -> TangemLogger.e("buyback failed: $error") }

        // Any outcome may have moved money; refreshed alongside, so the answer does not wait for it
        appScope.launch { accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(userWallet.walletId)) }

        return result
    }

    private suspend fun Raise<BuybackError>.generateTx(
        userWallet: UserWallet,
        nftAddress: String,
    ): CollectibleBuybackTx {
        val owner = ownerDeriver.storedOwner(userWallet.walletId) ?: raise(BuybackError.Unexpected)
        val vault = gachaVaultRepository.getVaultAddress(userWallet).getOrElse { raise(BuybackError.Unexpected) }

        return runSuspendCatching {
            collectiblesRepository.generateBuybackTx(
                userWalletId = userWallet.walletId,
                params = GenerateBuybackTxParams(fromAddress = owner, nftAddress = nftAddress, toAddress = vault),
            )
        }.getOrElse { raise(BuybackError.Unexpected) }
    }

    /** A lost report is not fatal: the status poll tells whether the backend saw the transfer. */
    private suspend fun reportTxHash(userWallet: UserWallet, buybackId: String, txHash: String) {
        runSuspendCatching { collectiblesRepository.reportBuybackTxSent(userWallet.walletId, buybackId, txHash) }
            .onFailure { TangemLogger.e("buyback: tx-sent report failed", it) }
    }

    private suspend fun Raise<BuybackError>.awaitConfirmation(userWallet: UserWallet, buybackId: String) {
        repeat(CONFIRMATION_POLLS) {
            delay(CONFIRMATION_POLL_INTERVAL)
            when (status(userWallet, buybackId)) {
                CollectibleBuybackStatus.CONFIRMED -> return
                CollectibleBuybackStatus.EXPIRED -> raise(BuybackError.NotConfirmed)
                else -> Unit
            }
        }

        raise(BuybackError.NotConfirmed)
    }

    private suspend fun status(userWallet: UserWallet, buybackId: String): CollectibleBuybackStatus? =
        runSuspendCatching { collectiblesRepository.getBuybackStatus(userWallet.walletId, buybackId) }
            .onFailure { TangemLogger.e("buyback: status request failed", it) }
            .getOrNull()

    private fun CollectiblesAccountError.toBuybackError(): BuybackError = when (this) {
        is CollectiblesAccountError.Cancelled -> BuybackError.Cancelled
        is CollectiblesAccountError.CardError -> BuybackError.CardRefused
        else -> BuybackError.Unexpected
    }

    private companion object {

        const val CONFIRMATION_POLLS = 5
        val CONFIRMATION_POLL_INTERVAL = 2.seconds
    }
}