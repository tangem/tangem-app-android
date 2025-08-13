package com.tangem.tap.domain.visa

import arrow.core.getOrElse
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.core.error.ext.tangemError
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.domain.card.common.visa.VisaWalletPublicKeyUtility
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.error.VisaCardScanError
import com.tangem.domain.visa.model.*
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.operations.attestation.AttestCardKeyCommand
import com.tangem.operations.attestation.AttestCardKeyResponse
import com.tangem.operations.attestation.AttestWalletKeyResponse
import com.tangem.operations.attestation.AttestWalletKeyTask
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume

internal class VisaCardScanHandler @Inject constructor(
    private val visaAuthRepository: VisaAuthRepository,
    private val visaActivationRepositoryFactory: VisaActivationRepository.Factory,
    private val visaAuthTokenStorage: VisaAuthTokenStorage,
) {

    private class SessionContext(
        val visaActivationRepository: VisaActivationRepository,
        val session: CardSession,
    )

    suspend fun handleVisaCardScan(session: CardSession): CompletionResult<VisaCardActivationStatus> {
        Timber.i("Attempting to handle Visa card scan")

        val card = session.environment.card ?: run {
            Timber.e("Card is null")
            return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        }

        val visaActivationRepository = visaActivationRepositoryFactory.create(
            VisaCardId(
                cardId = card.cardId,
                cardPublicKey = card.cardPublicKey.toHexString(),
            ),
        )

        val context = SessionContext(
            visaActivationRepository = visaActivationRepository,
            session = session,
        )

        card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: run {
            val activationInput =
                VisaActivationInput(card.cardId, card.cardPublicKey.toHexString(), card.isAccessCodeSet)
            val activationStatus = VisaCardActivationStatus.NotStartedActivation(activationInput)
            return CompletionResult.Success(activationStatus)
        }

        return context.handleWalletAuthorization()
    }

    private suspend fun SessionContext.handleWalletAuthorization(): CompletionResult<VisaCardActivationStatus> {
        Timber.i("Started handling authorization using Visa wallet")

        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        val wallet = card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: run {
            Timber.e("Failed to find extended public key while handling wallet authorization")
            return CompletionResult.Failure(VisaCardScanError.FailedToFindWallet.tangemError)
        }

        val walletAddress = VisaWalletPublicKeyUtility.generateAddressOnSecp256k1(wallet.publicKey)
            .getOrElse {
                return CompletionResult.Failure(it.tangemError)
            }

        Timber.i("Requesting challenge for wallet authorization")

        val challengeResponse = visaAuthRepository.getCardWalletAuthChallenge(
            cardId = card.cardId,
            // This is the wallet public key, not the address and it's alright, as the API expects it in this format
            cardWalletAddress = wallet.publicKey.toHexString(),
        ).getOrElse {
            Timber.i("Failed to get Access token for Wallet public key authorization")
            return CompletionResult.Failure(it.tangemError)
        }

        val signChallengeResult = signChallengeWithWallet(
            publicKey = wallet.publicKey,
            nonce = challengeResponse.challenge,
        )

        return when (signChallengeResult) {
            is CompletionResult.Success -> {
                val signature = signChallengeResult.data.walletSignature
                val salt = signChallengeResult.data.salt

                Timber.i("Challenge signed with Wallet public key")
                handleWalletAuthorizationTokens(
                    cardWalletAddress = walletAddress.value,
                    signedChallenge = challengeResponse.toSignedChallenge(
                        signedChallenge = signature.toHexString(),
                        salt = salt.toHexString(),
                    ),
                )
            }
            is CompletionResult.Failure -> {
                Timber.e("Error during Wallet authorization process. Tangem Sdk Error: ${signChallengeResult.error}")
                CompletionResult.Failure(signChallengeResult.error)
            }
        }
    }

    private suspend fun SessionContext.handleWalletAuthorizationTokens(
        cardWalletAddress: String,
        signedChallenge: VisaAuthSignedChallenge,
    ): CompletionResult<VisaCardActivationStatus> {
        val authorizationTokensResponse = visaAuthRepository.getAccessTokens(signedChallenge = signedChallenge)
            .getOrElse {
                Timber.i("Failed to get Access token for Wallet public key authorization.")
                return if (
                    it is VisaApiError.ProductInstanceIsNotActivated ||
                    it is VisaApiError.ProductInstanceNotFoundActivationRequired
                ) {
                    Timber.i("Proceeding with card authorization.")
                    handleCardAuthorization(cardWalletAddress = cardWalletAddress)
                } else {
                    CompletionResult.Failure(it.tangemError)
                }
            }

        Timber.i("Authorized using Wallet public key successfully")

        return CompletionResult.Success(VisaCardActivationStatus.Activated(authorizationTokensResponse))
    }

    @Suppress("LongMethod")
    private suspend fun SessionContext.handleCardAuthorization(
        cardWalletAddress: String,
    ): CompletionResult<VisaCardActivationStatus> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        Timber.i("Requesting authorization challenge to sign")

        val challengeResponse = visaAuthRepository.getCardAuthChallenge(
            cardId = card.cardId,
            cardPublicKey = card.cardPublicKey.toHexString(),
        ).getOrElse {
            Timber.e("Failed to get challenge for Card authorization. Plain error: ${it.errorCode}")
            return CompletionResult.Failure(it.tangemError)
        }

        Timber.i("Received challenge to sign: ${challengeResponse.challenge}")

        val signChallengeResult = signChallengeWithCard(challenge = challengeResponse.challenge)

        val attestCardKeyResponse = when (signChallengeResult) {
            is CompletionResult.Success -> {
                Timber.i("Challenged signed.")
                signChallengeResult.data
            }
            is CompletionResult.Failure -> {
                Timber.e(
                    "Failed to sign challenge with Card public key. Tangem Sdk Error: ${signChallengeResult.error}",
                )
                return CompletionResult.Failure(signChallengeResult.error)
            }
        }

        val authorizationTokensResponse = visaAuthRepository.getAccessTokens(
            signedChallenge = challengeResponse.toSignedChallenge(
                signedChallenge = attestCardKeyResponse.cardSignature.toHexString(),
                salt = attestCardKeyResponse.salt.toHexString(),
            ),
        ).getOrElse {
            Timber.e("Failed to sign challenge with Card public key. Plain error: ${it.errorCode}")
            return CompletionResult.Failure(it.tangemError)
        }

        visaAuthTokenStorage.store(
            cardId = card.cardId,
            tokens = authorizationTokensResponse,
        )

        val activationRemoteState = visaActivationRepository.getActivationRemoteState().getOrElse {
            Timber.e("Failed to sign challenge with Card public key. Plain error: ${it.errorCode}")
            return CompletionResult.Failure(it.tangemError)
        }

        val error = when (activationRemoteState) {
            VisaActivationRemoteState.BlockedForActivation -> VisaActivationError.BlockedForActivation
            VisaActivationRemoteState.Activated -> VisaActivationError.InvalidActivationState
            VisaActivationRemoteState.Failed -> VisaActivationError.FailedRemoteState
            else -> null
        }

        if (error != null) {
            return CompletionResult.Failure(error.tangemError)
        }

        val activationInput = VisaActivationInput(
            cardId = card.cardId,
            cardPublicKey = card.cardPublicKey.toHexString(),
            isAccessCodeSet = card.isAccessCodeSet,
        )

        return CompletionResult.Success(
            VisaCardActivationStatus.ActivationStarted(
                activationInput = activationInput,
                authTokens = authorizationTokensResponse,
                remoteState = activationRemoteState,
                cardWalletAddress = cardWalletAddress,
            ),
        )
    }

    private suspend fun SessionContext.signChallengeWithWallet(
        publicKey: ByteArray,
        nonce: String,
    ): CompletionResult<AttestWalletKeyResponse> {
        val signHashCommand = AttestWalletKeyTask(
            publicKey = publicKey,
            challenge = nonce.hexToBytes(),
        )
        return suspendCancellableCoroutine {
            signHashCommand.run(session) { result ->
                it.resume(result)
            }
        }
    }

    private suspend fun SessionContext.signChallengeWithCard(
        challenge: String,
    ): CompletionResult<AttestCardKeyResponse> {
        val signHashCommand = AttestCardKeyCommand(challenge = challenge.hexToBytes())
        return suspendCancellableCoroutine { continuation ->
            signHashCommand.run(session) { result ->
                continuation.resume(result)
            }
        }
    }
}