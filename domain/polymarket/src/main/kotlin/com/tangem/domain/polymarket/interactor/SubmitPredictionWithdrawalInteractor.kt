package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketWithdrawRepository
import com.tangem.domain.polymarket.PredictionWithdrawalStore
import com.tangem.domain.polymarket.amount.PredictionCollateralAmount
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionTransferCallCodec
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import com.tangem.domain.polymarket.model.PredictionWithdrawalBatch
import com.tangem.domain.polymarket.model.PredictionWithdrawalReceipt
import com.tangem.domain.polymarket.model.PredictionWithdrawalRecord
import com.tangem.domain.polymarket.model.PreparedPredictionWithdrawal
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal

sealed interface PredictionWithdrawalFailure {

    data object AlreadyInFlight : PredictionWithdrawalFailure

    data object NothingToResend : PredictionWithdrawalFailure

    data object AmountRejected : PredictionWithdrawalFailure

    data object CalldataMismatch : PredictionWithdrawalFailure

    data object AddressUnavailable : PredictionWithdrawalFailure

    data object NonceUnavailable : PredictionWithdrawalFailure

    data class SigningFailed(val error: PolymarketSigningError) : PredictionWithdrawalFailure

    data object Expired : PredictionWithdrawalFailure

    data class Rejected(val error: PredictionWithdrawError) : PredictionWithdrawalFailure

    data class PersistenceFailed(val cause: Throwable) : PredictionWithdrawalFailure
}

class SubmitPredictionWithdrawalInteractor(
    private val bridgeRepository: PolymarketBridgeRepository,
    private val withdrawRepository: PolymarketWithdrawRepository,
    private val polymarketRepository: PolymarketRepository,
    private val signer: PolymarketTypedDataSigner,
    private val store: PredictionWithdrawalStore,
    private val codec: PredictionTransferCallCodec,
) {

    /**
     * @return a withdrawal that is built and read back but not yet signed; [PreparedPredictionWithdrawal.decoded]
     * holds the recipient and amount as read out of the calldata
     */
    suspend fun prepare(params: Params): Either<PredictionWithdrawalFailure, PreparedPredictionWithdrawal> = either {
        ensure(readRecord(params.userWalletId) == null) { PredictionWithdrawalFailure.AlreadyInFlight }

        val amountBaseUnits = PredictionCollateralAmount.toBaseUnits(params.amount)
            .getOrNull() ?: raise(PredictionWithdrawalFailure.AmountRejected)

        val bridgeAddress = bridgeRepository.createWithdrawAddress(
            walletId = params.walletId,
            depositWalletAddress = params.depositWalletAddress,
            toChainId = params.toChainId,
            toTokenAddress = params.toTokenAddress,
            recipientAddress = params.recipientAddress,
        ).getOrNull() ?: raise(PredictionWithdrawalFailure.AddressUnavailable)

        val call = codec.build(recipient = bridgeAddress, amountBaseUnits = amountBaseUnits)
        val decoded = codec.decode(call)
        ensureNotNull(decoded) { PredictionWithdrawalFailure.CalldataMismatch }
        ensure(decoded.recipient.equals(bridgeAddress, ignoreCase = true)) {
            PredictionWithdrawalFailure.CalldataMismatch
        }
        ensure(decoded.amountBaseUnits == amountBaseUnits) { PredictionWithdrawalFailure.CalldataMismatch }

        val nonce = polymarketRepository.getRelayerNonce(params.ownerAddress)
            .getOrNull() ?: raise(PredictionWithdrawalFailure.NonceUnavailable)

        PreparedPredictionWithdrawal(
            userWalletId = params.userWalletId,
            walletId = params.walletId,
            ownerAddress = params.ownerAddress,
            depositWalletAddress = params.depositWalletAddress,
            nonce = nonce.toString(),
            deadline = PolymarketRelayerDeadline.fromNow(),
            call = call,
            decoded = decoded,
        )
    }

    /**
     * @return the receipt of the accepted batch; the signed batch is persisted before it is submitted, so a
     * crash in between leaves a record [resend] can pick up
     */
    suspend fun confirm(
        prepared: PreparedPredictionWithdrawal,
    ): Either<PredictionWithdrawalFailure, PredictionWithdrawalReceipt> = either {
        ensure(!PolymarketRelayerDeadline.isExpired(prepared.deadline)) { PredictionWithdrawalFailure.Expired }

        val signature = signer.signRelayedBatch(
            userWalletId = prepared.userWalletId,
            payload = PolymarketApprovalsPayload(
                depositWalletAddress = prepared.depositWalletAddress,
                nonce = prepared.nonce,
                deadline = prepared.deadline,
                calls = listOf(prepared.call),
            ),
        ).getOrElse { raise(PredictionWithdrawalFailure.SigningFailed(it)) }

        val batch = PredictionWithdrawalBatch(
            ownerAddress = prepared.ownerAddress,
            depositWalletAddress = prepared.depositWalletAddress,
            nonce = prepared.nonce,
            deadline = prepared.deadline,
            call = prepared.call,
            signature = signature,
        )

        runSuspendCatching {
            store.save(prepared.userWalletId, PredictionWithdrawalRecord(batch = batch, transactionId = null))
        }.getOrElse { raise(PredictionWithdrawalFailure.PersistenceFailed(it)) }

        send(userWalletId = prepared.userWalletId, walletId = prepared.walletId, batch = batch)
    }

    /** @return the receipt for the persisted batch, submitted again as signed, with no new signature or nonce */
    suspend fun resend(
        userWalletId: UserWalletId,
        walletId: String,
    ): Either<PredictionWithdrawalFailure, PredictionWithdrawalReceipt> = either {
        val record = readRecord(userWalletId) ?: raise(PredictionWithdrawalFailure.NothingToResend)

        send(userWalletId = userWalletId, walletId = walletId, batch = record.batch)
    }

    private suspend fun Raise<PredictionWithdrawalFailure>.send(
        userWalletId: UserWalletId,
        walletId: String,
        batch: PredictionWithdrawalBatch,
    ): PredictionWithdrawalReceipt {
        val receipt = withdrawRepository.submit(walletId = walletId, batch = batch).getOrElse { error ->
            if (error.recordedNothing()) clearRecord(userWalletId)
            raise(PredictionWithdrawalFailure.Rejected(error))
        }

        runSuspendCatching {
            store.save(userWalletId, PredictionWithdrawalRecord(batch = batch, transactionId = receipt.transactionId))
        }.onFailure { TangemLogger.e("Failed to record the accepted withdrawal ${receipt.transactionId}", it) }

        return receipt
    }

    private suspend fun Raise<PredictionWithdrawalFailure>.readRecord(
        userWalletId: UserWalletId,
    ): PredictionWithdrawalRecord? = runSuspendCatching { store.get(userWalletId) }
        .getOrElse { raise(PredictionWithdrawalFailure.PersistenceFailed(it)) }

    private suspend fun clearRecord(userWalletId: UserWalletId) {
        runSuspendCatching { store.clear(userWalletId) }
            .onFailure { TangemLogger.e("Failed to clear the withdrawal the relayer did not record", it) }
    }

    private fun PredictionWithdrawError.recordedNothing(): Boolean =
        this is PredictionWithdrawError.RelayerRejected || this is PredictionWithdrawError.InvalidRequest

    data class Params(
        val userWalletId: UserWalletId,
        val walletId: String,
        val ownerAddress: String,
        val depositWalletAddress: String,
        val amount: BigDecimal,
        val toChainId: String,
        val toTokenAddress: String,
        val recipientAddress: String,
    )
}