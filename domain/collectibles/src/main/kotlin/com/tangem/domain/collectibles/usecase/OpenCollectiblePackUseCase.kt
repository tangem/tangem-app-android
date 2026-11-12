package com.tangem.domain.collectibles.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.delegate.CollectiblesSessionKey
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsFetcher
import com.tangem.domain.collectibles.models.CollectibleException
import com.tangem.domain.collectibles.models.CollectibleTurboMode
import com.tangem.domain.collectibles.models.CollectibleTxState
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.domain.collectibles.models.GeneratePurchaseTxParams
import com.tangem.domain.collectibles.models.OpenPackRequest
import com.tangem.domain.collectibles.models.OpenPackStep
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Buys and opens one pack: the backend issues the spin, the vault pays for it on chain, the gacha sees the payment,
 * the pack is opened.
 *
 * Nothing is spent until the payment is sent. An expired purchase or a pack the gacha refuses ends as
 * [OpenPackStep.Failed.OpenFailed]; any other failure to open is [OpenPackStep.Failed.PackUnopened], which
 * [OpenPackRequest.Reopen] retries as many times as asked.
 *
 * The vault must still answer to this device's session key: it is checked before the spin is issued and again when
 * the payment fails, and a vault re-pointed by another phone marks the account for a relink.
 */
@Suppress("LongParameterList")
class OpenCollectiblePackUseCase @Inject constructor(
    private val collectiblesRepository: CollectiblesRepository,
    private val gachaVaultRepository: GachaVaultRepository,
    private val ownerDeriver: CollectiblesOwnerDeriver,
    private val accountStatusFetcher: CollectiblesAccountStatusFetcher,
    private val activeTxsFetcher: CollectiblesActiveTxsFetcher,
    private val accountRepository: CollectiblesAccountRepository,
    private val sessionKey: CollectiblesSessionKey,
) {

    /** @param isTurboEnabled the pack is bought in Turbo mode, so its card may come back auto-sold instead of opened */
    operator fun invoke(
        userWallet: UserWallet,
        request: OpenPackRequest,
        isTurboEnabled: Boolean,
    ): Flow<OpenPackStep> = flow {
        val result = either {
            val txId = when (request) {
                is OpenPackRequest.Buy -> {
                    val sent = buyPack(
                        userWallet = userWallet,
                        request = request,
                        isTurboEnabled = isTurboEnabled,
                        onStep = { step -> emit(step) },
                    )
                    reportTxHash(userWallet, sent)
                    sent.txId
                }
                is OpenPackRequest.Reopen -> {
                    emit(OpenPackStep.Confirming)
                    request.txId
                }
            }

            val purchaseState = awaitPurchase(userWallet, txId, isTurboEnabled)
            if (purchaseState.status == CollectibleTxStatus.AUTO_BUYBACKED) return@either autoBuybacked(purchaseState)

            emit(OpenPackStep.Opening)
            openPack(userWallet, txId, isTurboEnabled)
        }

        coroutineScope {
            launch { accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(userWallet.walletId)) }
            launch { activeTxsFetcher(CollectiblesActiveTxsFetcher.Params(userWallet.walletId)) }
            emit(result.getOrElse { it })
        }
    }

    private suspend fun Raise<OpenPackStep.Failed>.buyPack(
        userWallet: UserWallet,
        request: OpenPackRequest.Buy,
        isTurboEnabled: Boolean,
        onStep: suspend (OpenPackStep) -> Unit,
    ): SentPurchase {
        onStep(OpenPackStep.Generating)
        val owner = ownerDeriver.storedOwner(userWallet.walletId)
            ?: fail(OpenPackStep.Failed.PurchaseFailed, cause = "owner key is not stored")
        ensureSessionKeyLinked(userWallet)

        val purchase = runSuspendCatching {
            collectiblesRepository.generatePurchaseTx(
                userWalletId = userWallet.walletId,
                params = GeneratePurchaseTxParams(
                    fromAddress = owner,
                    machineId = request.machineId,
                    // The vault pays, so it is the player; Day 1 delivers the prize to the owner (the card's key)
                    toAddress = owner,
                    collectibleTurboMode = if (isTurboEnabled) turboMode(userWallet) else null,
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
            amount = purchase.amount,
            feeAmount = purchase.feeAmount,
            memo = purchase.memo,
        ).getOrElse { error ->
            ensureSessionKeyLinked(userWallet)
            fail(OpenPackStep.Failed.PurchaseFailed, cause = error)
        }

        return SentPurchase(txId = purchase.txId, txHash = txHash)
    }

    private suspend fun Raise<OpenPackStep.Failed>.turboMode(userWallet: UserWallet): CollectibleTurboMode {
        val vault = gachaVaultRepository.getVaultAddress(userWallet)
            .getOrElse { error -> fail(OpenPackStep.Failed.PurchaseFailed, cause = error) }

        return CollectibleTurboMode(isEnabled = true, buybackAddress = vault)
    }

    /** A vault that cannot be read is not paid from: whether it still answers to this device is unknown. */
    private suspend fun Raise<OpenPackStep.Failed>.ensureSessionKeyLinked(userWallet: UserWallet) {
        val vaultSessionKey = gachaVaultRepository.getVaultSessionKey(userWallet)
            .getOrElse { error -> fail(OpenPackStep.Failed.PurchaseFailed, cause = error) }
        val accountId = AccountId.forCollectiblesAccount(userWalletId = userWallet.walletId)
        val localSessionKey = sessionKey.getPublicKeyOrNull(accountId)?.base58

        if (vaultSessionKey == null || vaultSessionKey == localSessionKey) return

        if (accountRepository.isRelinkPending(userWallet.walletId)) {
            fail(OpenPackStep.Failed.PurchaseFailed, cause = "the relink has not landed yet")
        }

        accountRepository.saveAccount(
            userWalletId = userWallet.walletId,
            status = CollectiblesAccountStatusValue.UpdateRequired(
                source = StatusSource.ACTUAL,
                vaultBalance = accountRepository.getStatusSync(userWallet.walletId)?.balance,
            ),
        )
        fail(OpenPackStep.Failed.PurchaseFailed, cause = "the vault answers to another session key")
    }

    /** Not fatal: the gacha finds the payment by its memo, so the wait for it goes on either way. */
    private suspend fun reportTxHash(userWallet: UserWallet, sent: SentPurchase) {
        runSuspendCatching {
            collectiblesRepository.reportPurchaseTxSent(
                userWalletId = userWallet.walletId,
                txId = sent.txId,
                txHash = sent.txHash,
            )
        }.onFailure { TangemLogger.e("open pack: reporting the tx hash failed", it) }
    }

    /** A status request that fails is just another poll: only the deadline or a final status ends the wait. */
    private suspend fun Raise<OpenPackStep.Failed>.awaitPurchase(
        userWallet: UserWallet,
        txId: String,
        isTurboEnabled: Boolean,
    ): CollectibleTxState {
        val state = withTimeoutOrNull(PAYMENT_TIMEOUT) { pollUntilSettled(userWallet, txId, isTurboEnabled) }
            ?: fail(OpenPackStep.Failed.PackUnopened(txId), cause = "payment timeout")

        failIfClosed(state.status)

        return state
    }

    private suspend fun pollUntilSettled(
        userWallet: UserWallet,
        txId: String,
        isTurboEnabled: Boolean,
    ): CollectibleTxState {
        while (true) {
            val state = txState(userWallet, txId, isTurboEnabled)
            if (state != null && !state.status.isPaymentInProgress) return state
            delay(POLL_INTERVAL)
        }
    }

    private suspend fun Raise<OpenPackStep.Failed>.openPack(
        userWallet: UserWallet,
        txId: String,
        isTurboEnabled: Boolean,
    ): OpenPackStep {
        val state = runSuspendCatching {
            collectiblesRepository.openPack(
                userWalletId = userWallet.walletId,
                txId = txId,
                includeAutoBuyback = isTurboEnabled,
            )
        }.getOrElse { cause ->
            val failure = when (cause) {
                is CollectibleException.PackOpeningRefused -> OpenPackStep.Failed.OpenFailed
                is CollectibleException.RefundInProgress -> OpenPackStep.Failed.Refunding
                else -> OpenPackStep.Failed.PackUnopened(txId)
            }
            fail(failure, cause = cause)
        }

        if (state.status == CollectibleTxStatus.AUTO_BUYBACKED) return autoBuybacked(state)

        val pack = state.pack ?: run {
            failIfClosed(state.status)
            fail(OpenPackStep.Failed.PackUnopened(txId), cause = "no nftWon in ${state.status}")
        }

        return OpenPackStep.Opened(pack)
    }

    private fun Raise<OpenPackStep.Failed>.autoBuybacked(state: CollectibleTxState): OpenPackStep.AutoBuybacked {
        val amount = state.autoBuybackAmount
            ?: fail(OpenPackStep.Failed.Unexpected, cause = "auto-buybacked without a payout")

        return OpenPackStep.AutoBuybacked(amount = amount, pack = state.pack)
    }

    private fun Raise<OpenPackStep.Failed>.failIfClosed(status: CollectibleTxStatus) {
        when (status) {
            CollectibleTxStatus.EXPIRED,
            CollectibleTxStatus.PACK_OPENING_ERROR,
            CollectibleTxStatus.PAUSED,
            -> fail(OpenPackStep.Failed.OpenFailed, cause = status)
            CollectibleTxStatus.REFUNDED -> fail(OpenPackStep.Failed.Refunded, cause = status)
            CollectibleTxStatus.REFUNDING -> fail(OpenPackStep.Failed.Refunding, cause = status)
            CollectibleTxStatus.MANUALLY_BUYBACKED -> fail(OpenPackStep.Failed.Unexpected, cause = status)
            else -> Unit
        }
    }

    private suspend fun txState(userWallet: UserWallet, txId: String, isTurboEnabled: Boolean): CollectibleTxState? =
        runSuspendCatching {
            collectiblesRepository.getTxState(
                userWalletId = userWallet.walletId,
                txId = txId,
                includeAutoBuyback = isTurboEnabled,
            )
        }
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
    }
}