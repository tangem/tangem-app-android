package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyBatch
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.model.PolymarketSignedOnboarding
import com.tangem.domain.polymarket.model.PolymarketSignedSessionKeyBatches
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyCalls
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketClobAuthData
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline
import com.tangem.domain.polymarket.signing.PolymarketSignRequest
import com.tangem.domain.polymarket.signing.PolymarketSigned
import com.tangem.domain.polymarket.signing.PolymarketSigningSession
import com.tangem.domain.polymarket.signing.PolymarketTypedPayload
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketRelayerNonceUseCase
import kotlinx.coroutines.withTimeoutOrNull

class SignPolymarketSessionKeyBatchesInteractor(
    private val signingSession: PolymarketSigningSession,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val getRelayerNonce: GetPolymarketRelayerNonceUseCase,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        batches: NonEmptyList<PolymarketSessionKeyBatch>,
    ): Either<PolymarketSessionKeyError, PolymarketSignedSessionKeyBatches> {
        val isOwnerClobAuthNeeded = getApiCredentials(userWalletId) == null
        return signingSession
            .run(userWalletId) { ownerAddress ->
                val prepared = prepare(
                    userWalletId = userWalletId,
                    ownerAddress = ownerAddress,
                    batches = batches,
                    isOwnerClobAuthNeeded = isOwnerClobAuthNeeded,
                )
                PolymarketSignRequest(value = prepared, payloads = prepared.getOrNull()?.payloads.orEmpty())
            }
            .mapLeft(PolymarketSessionKeyError::SigningFailed)
            .flatMap { signed -> signed.value.flatMap { prepared -> prepared.toSigned(signed) } }
    }

    private suspend fun prepare(
        userWalletId: UserWalletId,
        ownerAddress: String,
        batches: NonEmptyList<PolymarketSessionKeyBatch>,
        isOwnerClobAuthNeeded: Boolean,
    ): Either<PolymarketSessionKeyError, Prepared> = either {
        val addresses = deriveAddresses.fromOwner(userWalletId, ownerAddress)
            .mapLeft(PolymarketSessionKeyError::PreparationFailed)
            .bind()
        val nonce = withTimeoutOrNull(NETWORK_IN_SESSION_TIMEOUT_MILLIS) { getRelayerNonce(addresses) }
            .let { it ?: PolymarketOnboardingError.Network.left() }
            .mapLeft(PolymarketSessionKeyError::PreparationFailed)
            .bind()
        val now = nowMillis()
        val validUntil = PolymarketSessionKeyCalls.validUntil(now)
        Prepared(
            addresses = addresses,
            validUntil = validUntil,
            ownerClobAuth = PolymarketClobAuthData(timestamp = (now / MILLIS_IN_SECOND).toString())
                .takeIf { isOwnerClobAuthNeeded },
            batches = batches.mapIndexed { index, batch ->
                PreparedBatch(
                    request = batch,
                    payload = PolymarketApprovalsPayload(
                        depositWalletAddress = addresses.depositWalletAddress,
                        nonce = (nonce + index.toBigInteger()).toString(),
                        deadline = PolymarketRelayerDeadline.fromNow(
                            nowMillis = now,
                            windowSeconds = if (index == 0) {
                                PolymarketRelayerDeadline.WINDOW_SECONDS
                            } else {
                                PolymarketRelayerDeadline.FOLLOW_UP_WINDOW_SECONDS
                            },
                        ),
                        calls = listOf(call(batch, validUntil, addresses.depositWalletAddress)),
                    ),
                )
            },
        )
    }

    private fun Raise<PolymarketSessionKeyError>.call(
        batch: PolymarketSessionKeyBatch,
        validUntil: Long,
        depositWalletAddress: String,
    ) = when (batch) {
        is PolymarketSessionKeyBatch.Authorize -> PolymarketSessionKeyCalls.authorize(
            sessionKeyAddress = batch.sessionKeyAddress,
            validUntil = validUntil,
            depositWalletAddress = depositWalletAddress,
        )
        is PolymarketSessionKeyBatch.Revoke -> PolymarketSessionKeyCalls.revoke(
            sessionKeyAddress = batch.sessionKeyAddress,
            depositWalletAddress = depositWalletAddress,
        )
    }.bind()

    private fun Prepared.toSigned(
        signed: PolymarketSigned<*>,
    ): Either<PolymarketSessionKeyError, PolymarketSignedSessionKeyBatches> = either {
        ensure(signed.signatures.size == payloads.size) {
            PolymarketSessionKeyError.SigningFailed(PolymarketSigningError.Unknown)
        }
        val signatures = signed.signatures.iterator()
        PolymarketSignedSessionKeyBatches(
            ownerClobAuth = ownerClobAuth?.let { data ->
                PolymarketSignedOnboarding.SignedClobAuth(l1Signature = signatures.next(), timestamp = data.timestamp)
            },
            batches = batches.map { batch -> signedBatch(batch = batch, signature = signatures.next()) },
        )
    }

    private fun Prepared.signedBatch(
        batch: PreparedBatch,
        signature: String,
    ): PolymarketSignedSessionKeyBatches.Signed = when (val request = batch.request) {
        is PolymarketSessionKeyBatch.Authorize -> PolymarketSignedSessionKeyBatches.Signed.Authorization(
            PolymarketSessionKeyAuthorization(
                ownerAddress = addresses.ownerAddress,
                depositWalletAddress = addresses.depositWalletAddress,
                sessionKeyAddress = request.sessionKeyAddress,
                deviceId = request.deviceId,
                deviceName = request.deviceName,
                validUntil = validUntil.toString(),
                nonce = batch.payload.nonce,
                deadline = batch.payload.deadline,
                call = batch.payload.calls.single(),
                signature = signature,
                replacesSessionKeyAddress = request.replacesSessionKeyAddress,
            ),
        )
        is PolymarketSessionKeyBatch.Revoke -> PolymarketSignedSessionKeyBatches.Signed.Revocation(
            PolymarketSessionKeyRevocation(
                ownerAddress = addresses.ownerAddress,
                depositWalletAddress = addresses.depositWalletAddress,
                sessionKeyAddress = request.sessionKeyAddress,
                nonce = batch.payload.nonce,
                deadline = batch.payload.deadline,
                call = batch.payload.calls.single(),
                signature = signature,
            ),
        )
    }

    private class PreparedBatch(val request: PolymarketSessionKeyBatch, val payload: PolymarketApprovalsPayload)

    private class Prepared(
        val addresses: PolymarketAddresses,
        val validUntil: Long,
        val ownerClobAuth: PolymarketClobAuthData?,
        val batches: List<PreparedBatch>,
    ) {

        val payloads: List<PolymarketTypedPayload>
            get() = listOfNotNull(ownerClobAuth?.let(PolymarketTypedPayload::ClobAuth)) +
                batches.map { batch -> PolymarketTypedPayload.RelayedBatch(batch.payload) }
    }

    private companion object {
        const val MILLIS_IN_SECOND = 1_000L
        const val NETWORK_IN_SESSION_TIMEOUT_MILLIS = 10_000L
    }
}