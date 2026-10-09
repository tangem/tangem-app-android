package com.tangem.domain.collectibles.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import com.tangem.domain.collectibles.models.CollectibleException
import com.tangem.domain.collectibles.models.CollectibleTurboMode
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.domain.collectibles.models.GeneratePurchaseTxParams
import com.tangem.domain.collectibles.models.OpenPackRequest
import com.tangem.domain.collectibles.models.OpenPackStep
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Buys and opens one pack: the backend issues the spin, the vault pays for it on chain, the gacha sees the payment,
 * the pack is opened.
 *
 * Nothing is spent until the payment is sent; once it is, any failure is [OpenPackStep.Failed.PackUnopened], which
 * [OpenPackRequest.Reopen] retries.
 */
class OpenCollectiblePackUseCase @Inject constructor(
    private val collectiblesRepository: CollectiblesRepository,
    private val gachaVaultRepository: GachaVaultRepository,
    private val hotDelegate: CollectiblesHotDelegate,
    private val ownerDeriver: CollectiblesOwnerDeriver,
) {

    operator fun invoke(userWallet: UserWallet, request: OpenPackRequest): Flow<OpenPackStep> = flow {
        val result = either {
            val txId = when (request) {
                is OpenPackRequest.Buy -> {
                    val sent = buyPack(userWallet, request, onStep = { step -> emit(step) })
                    reportTxHash(userWallet, sent)
                    sent.txId
                }
                is OpenPackRequest.Reopen -> {
                    emit(OpenPackStep.Confirming)
                    request.txId
                }
            }

            awaitPurchase(userWallet, txId)

            emit(OpenPackStep.Opening)
            openPack(userWallet, txId)
        }

        emit(result.fold(ifLeft = { it }, ifRight = { OpenPackStep.Opened(it) }))
    }

    private suspend fun Raise<OpenPackStep.Failed>.buyPack(
        userWallet: UserWallet,
        request: OpenPackRequest.Buy,
        onStep: suspend (OpenPackStep) -> Unit,
    ): SentPurchase {
        onStep(OpenPackStep.Generating)
        val delegate = hotDelegate.getPublicKeyOrNull(AccountId.forCollectiblesAccount(userWallet.walletId))
            ?: fail(OpenPackStep.Failed.PurchaseFailed, cause = "hot delegate is not readable")

        val owner = ownerDeriver.storedOwner(userWallet.walletId)
            ?: fail(OpenPackStep.Failed.PurchaseFailed, cause = "owner key is not stored")
        val vault = gachaVaultRepository.getVaultAddress(userWallet)
            .getOrElse { fail(OpenPackStep.Failed.PurchaseFailed, cause = it) }

        val purchase = runSuspendCatching {
            collectiblesRepository.generatePurchaseTx(
                userWalletId = userWallet.walletId,
                params = GeneratePurchaseTxParams(
                    fromAddress = vault, // TODO waiting for info from Stepan
                    machineId = request.machineId,
                    // The vault pays, so it is the player; Day 1 delivers the prize to the owner (the card's key)
                    toAddress = owner,
                    collectibleTurboMode = CollectibleTurboMode(isEnabled = true, buybackAddress = owner)
                        .takeIf { request.isTurboEnabled },
                ),
            )
        }.getOrElse { cause ->
            val failure = when (cause) {
                is CollectibleException.LowInventory -> OpenPackStep.Failed.LowInventory
                else -> OpenPackStep.Failed.PurchaseFailed
            }
            fail(failure, cause = cause)
        }

        onStep(OpenPackStep.Confirming)

        val txHash = gachaVaultRepository.sendPurchasePack(
            userWallet = userWallet,
            hotDelegate = delegate,
            amount = purchase.amount,
            feeAmount = purchase.feeAmount,
            memo = purchase.memo,
        ).getOrElse { fail(OpenPackStep.Failed.PurchaseFailed, cause = it) }

        return SentPurchase(txId = purchase.txId, txHash = txHash)
    }

    private suspend fun Raise<OpenPackStep.Failed>.reportTxHash(userWallet: UserWallet, sent: SentPurchase) {
        runSuspendCatching {
            collectiblesRepository.reportPurchaseTxSent(
                userWalletId = userWallet.walletId,
                txId = sent.txId,
                txHash = sent.txHash,
            )
        }.getOrElse { fail(OpenPackStep.Failed.PackUnopened(sent.txId), cause = it) }
    }

    /** A status request that fails is just another poll: only the deadline or a final status ends the wait. */
    private suspend fun Raise<OpenPackStep.Failed>.awaitPurchase(userWallet: UserWallet, txId: String) {
        val status = withTimeoutOrNull(PAYMENT_TIMEOUT) { pollUntilSettled(userWallet, txId) }
            ?: fail(OpenPackStep.Failed.PackUnopened(txId), cause = "payment timeout")

        // TODO a refunded or expired purchase is not "paid, left unopened"; agree on what the user sees
        if (status in NOT_PURCHASED) fail(OpenPackStep.Failed.PackUnopened(txId), cause = status)
    }

    private suspend fun pollUntilSettled(userWallet: UserWallet, txId: String): CollectibleTxStatus {
        while (true) {
            val status = txStatus(userWallet, txId)
            if (status != null && status !in IN_PROGRESS) return status
            delay(POLL_INTERVAL)
        }
    }

    /** The opening answers with the card, so it is shown at once. */
    private suspend fun Raise<OpenPackStep.Failed>.openPack(
        userWallet: UserWallet,
        txId: String,
    ): OpenedCollectiblePack {
        val state = runSuspendCatching { collectiblesRepository.openPack(userWallet.walletId, txId) }
            .getOrElse { fail(OpenPackStep.Failed.PackUnopened(txId), cause = it) }

        return state.pack ?: fail(OpenPackStep.Failed.PackUnopened(txId), cause = "no nftWon")
    }

    private suspend fun txStatus(userWallet: UserWallet, txId: String): CollectibleTxStatus? =
        runSuspendCatching { collectiblesRepository.getTxState(userWallet.walletId, txId).status }
            .onFailure { TangemLogger.e("open pack: status request failed", it) }
            .getOrNull()

    private fun Raise<OpenPackStep.Failed>.fail(failure: OpenPackStep.Failed, cause: Any?): Nothing {
        TangemLogger.e("open pack: ${failure::class.simpleName}, cause=$cause")
        raise(failure)
    }

    private data class SentPurchase(val txId: String, val txHash: String)

    private companion object {

        val POLL_INTERVAL = 2.seconds
        val PAYMENT_TIMEOUT = 100.seconds

        val NOT_PURCHASED = setOf(
            CollectibleTxStatus.REFUNDED,
            CollectibleTxStatus.EXPIRED,
            // TODO turbo mode: an auto-sold pack is a result of its own, not a failure
            CollectibleTxStatus.AUTO_BUYBACKED,
            CollectibleTxStatus.PAUSED,
        )

        val IN_PROGRESS = setOf(
            CollectibleTxStatus.CREATED,
            CollectibleTxStatus.WAITING_TX_HASH,
            CollectibleTxStatus.PURCHASE_TX_HASH,
            CollectibleTxStatus.WAITING_FOR_PAYMENT,
            CollectibleTxStatus.UNKNOWN,
        )
    }
}