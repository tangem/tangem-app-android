package com.tangem.tap.domain.visa

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.core.CardSession
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.domain.common.visa.VisaUtilities
import com.tangem.domain.visa.model.*
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.operations.attestation.AttestCardKeyCommand
import com.tangem.operations.attestation.AttestCardKeyResponse
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
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
        val cardId: String,
        val session: CardSession,
    )

    suspend fun handleVisaCardScan(session: CardSession): CompletionResult<VisaCardActivationStatus> {
        Timber.i("Attempting to handle Visa card scan")

        val card = session.environment.card ?: run {
            Timber.e("Card is null")
            return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        }

        val visaActivationRepository = visaActivationRepositoryFactory.create(card.cardId)

        val context = SessionContext(
            visaActivationRepository = visaActivationRepository,
            cardId = card.cardId,
            session = session,
        )

        val wallet = card.wallets.firstOrNull { it.curve == VisaUtilities.mandatoryCurve } ?: run {
            val activationInput =
                VisaActivationInput(card.cardId, card.cardPublicKey, card.isAccessCodeSet)
            val activationStatus = VisaCardActivationStatus.NotStartedActivation(activationInput)
            return CompletionResult.Success(activationStatus)
        }

        return context.deriveKey(wallet)
    }

    private suspend fun SessionContext.deriveKey(wallet: CardWallet): CompletionResult<VisaCardActivationStatus> {
        val derivationPath = VisaUtilities.visaDefaultDerivationPath ?: run {
            Timber.e("Failed to create derivation path while first scan")

            return CompletionResult.Failure(
                TangemSdkError.Underlying(VisaCardScanHandlerError.FailedToCreateDerivationPath.errorDescription),
            )
        }

        val derivationTask = DeriveWalletPublicKeyTask(wallet.publicKey, derivationPath)
        val derivationTaskResult = suspendCancellableCoroutine { continuation ->
            derivationTask.run(session) { result ->
                continuation.resume(result)
            }
        }
        return handleDerivationResponse(derivationTaskResult)
    }

    private suspend fun SessionContext.handleDerivationResponse(
        result: CompletionResult<ExtendedPublicKey>,
    ): CompletionResult<VisaCardActivationStatus> {
        return when (result) {
            is CompletionResult.Success -> {
                Timber.i("Start task for loading challenge for Visa wallet")
                handleWalletAuthorization()
            }
            is CompletionResult.Failure -> {
                CompletionResult.Failure(result.error)
            }
        }
    }

    private suspend fun SessionContext.handleWalletAuthorization(): CompletionResult<VisaCardActivationStatus> {
        Timber.i("Started handling authorization using Visa wallet")

        val derivationPath = VisaUtilities.visaDefaultDerivationPath ?: run {
            Timber.e("Failed to create derivation path while handling wallet authorization")
            return CompletionResult.Failure(
                TangemSdkError.Underlying(VisaCardScanHandlerError.FailedToCreateDerivationPath.errorDescription),
            )
        }

        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        val wallet = card.wallets.firstOrNull { it.curve == VisaUtilities.mandatoryCurve } ?: run {
            Timber.e("Failed to find extended public key while handling wallet authorization")
            return CompletionResult.Failure(
                TangemSdkError.Underlying(VisaCardScanHandlerError.FailedToFindDerivedWalletKey.errorDescription),
            )
        }

        val extendedPublicKey = wallet.derivedKeys[derivationPath] ?: run {
            Timber.e("Failed to find extended public key while handling wallet authorization")
            return CompletionResult.Failure(
                TangemSdkError.Underlying(VisaCardScanHandlerError.FailedToFindDerivedWalletKey.errorDescription),
            )
        }

        Timber.i("Requesting challenge for wallet authorization")
        val challengeResponse = runCatching {
            visaAuthRepository.getCustomerWalletAuthChallenge(
                cardId = card.cardId,
                walletPublicKey = extendedPublicKey.publicKey.toHexString(),
            )
        }.getOrElse {
            return CompletionResult.Failure(TangemSdkError.Underlying(it.message ?: "Unknown error"))
        }

        val signChallengeResult = signChallengeWithWallet(
            publicKey = wallet.publicKey,
            derivationPath = derivationPath,
            nonce = challengeResponse.challenge,
        )

        return when (signChallengeResult) {
            is CompletionResult.Success -> {
                Timber.i("Challenge signed with Wallet public key")
                handleWalletAuthorizationTokens(
                    signedChallenge = challengeResponse
                        .toSignedChallenge(signChallengeResult.data.signature.toHexString()),
                )
            }
            is CompletionResult.Failure -> {
                Timber.e("Error during Wallet authorization process. Tangem Sdk Error: ${signChallengeResult.error}")
                CompletionResult.Failure(signChallengeResult.error)
            }
        }
    }

    private suspend fun SessionContext.handleWalletAuthorizationTokens(
        signedChallenge: VisaAuthSignedChallenge,
    ): CompletionResult<VisaCardActivationStatus> {
        val authorizationTokensResponse = runCatching {
            visaAuthRepository.getAccessTokens(signedChallenge = signedChallenge)
        }.getOrElse {
            Timber.i(
                "Failed to get Access token for Wallet public key authoziation. Authorizing using Card Pub key",
            )
            return handleCardAuthorization()
        }

        visaAuthTokenStorage.store(
            cardId = cardId,
            tokens = authorizationTokensResponse,
        )

        Timber.i("Authorized using Wallet public key successfully")

        return CompletionResult.Success(VisaCardActivationStatus.Activated(authorizationTokensResponse))
    }

    private suspend fun SessionContext.handleCardAuthorization(): CompletionResult<VisaCardActivationStatus> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        Timber.i("Requesting authorization challenge to sign")

        val challengeResponse = runCatching {
            visaAuthRepository.getCardAuthChallenge(
                cardId = card.cardId,
                cardPublicKey = card.cardPublicKey.toHexString(),
            )
        }.getOrElse {
            Timber.e("Failed to get challenge for Card authorization. Plain error: ${it.message}")
            return CompletionResult.Failure(TangemSdkError.Underlying(it.message ?: "Unknown error"))
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

        @Suppress("UnusedPrivateMember")
        val authorizationTokensResponse = runCatching {
            visaAuthRepository.getAccessTokens(
                signedChallenge = challengeResponse.toSignedChallenge(
                    signedChallenge = attestCardKeyResponse.cardSignature.toHexString(),
                    salt = attestCardKeyResponse.salt.toHexString(),
                ),
            )
        }.getOrElse {
            Timber.e("Failed to sign challenge with Card public key. Plain error: ${it.message}")
            return CompletionResult.Failure(
                TangemSdkError.Underlying(
                    customMessage = it.message ?: "Unknown error",
                ),
            )
        }

        visaAuthTokenStorage.store(
            cardId = card.cardId,
            tokens = authorizationTokensResponse,
        )

        val activationRemoteState = visaActivationRepository.getActivationRemoteState()

        val error = when (activationRemoteState) {
            VisaActivationRemoteState.BlockedForActivation -> VisaActivationError.BlockedForActivation
            VisaActivationRemoteState.Activated -> VisaActivationError.InvalidActivationState
            else -> null
        }

        if (error != null) {
            return CompletionResult.Failure(TangemSdkError.Underlying(error.message))
        }

        val activationInput = VisaActivationInput(
            cardId = card.cardId,
            cardPublicKey = card.cardPublicKey,
            isAccessCodeSet = card.isAccessCodeSet,
        )

        return CompletionResult.Success(
            VisaCardActivationStatus.ActivationStarted(
                activationInput = activationInput,
                authTokens = authorizationTokensResponse,
                remoteState = activationRemoteState,
            ),
        )
    }

    private suspend fun SessionContext.signChallengeWithWallet(
        publicKey: ByteArray,
        derivationPath: DerivationPath,
        nonce: String,
    ): CompletionResult<SignHashResponse> {
        val signHashCommand = SignHashCommand(publicKey, nonce.toByteArray(), derivationPath)
        val result = suspendCancellableCoroutine {
            signHashCommand.run(session) { result ->
                it.resume(result)
            }
        }

        return result
    }

    private suspend fun SessionContext.signChallengeWithCard(
        challenge: String,
    ): CompletionResult<AttestCardKeyResponse> {
        val signHashCommand = AttestCardKeyCommand(challenge = challenge.toByteArray())
        val result = suspendCancellableCoroutine { continuation ->
            signHashCommand.run(session) { result ->
                continuation.resume(result)
            }
        }

        return when (result) {
            is CompletionResult.Success -> {
                CompletionResult.Success(result.data)
            }
            is CompletionResult.Failure -> {
                CompletionResult.Failure(result.error)
            }
        }
    }
}