package com.tangem.tap.domain.tasks.visa

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.map
import com.tangem.common.timemeasure.RealtimeMonotonicTimeSource
import com.tangem.crypto.CryptoUtils
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.datasource.local.visa.VisaOTPStorage
import com.tangem.datasource.local.visa.hasSavedOTP
import com.tangem.domain.common.visa.VisaUtilities
import com.tangem.domain.visa.model.*
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.operations.GenerateOTPCommand
import com.tangem.operations.attestation.AttestCardKeyCommand
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
import com.tangem.operations.wallet.CreateWalletTask
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.jvm.Throws
import kotlin.time.measureTimedValue

@Suppress("LongParameterList")
class VisaCardActivationTask @AssistedInject constructor(
    @Assisted private val accessCode: String,
    @Assisted private val challengeToSign: VisaAuthChallenge.Card?,
    @Assisted private val activationInput: VisaActivationInput,
    @Assisted private val coroutineScope: CoroutineScope,
    private val otpStorage: VisaOTPStorage,
    private val visaAuthTokenStorage: VisaAuthTokenStorage,
    private val visaAuthRepository: VisaAuthRepository,
    private val visaActivationRepositoryFactory: VisaActivationRepository.Factory,
) : CardSessionRunnable<VisaCardActivationResponse> {

    private class SessionContext(
        val visaActivationRepository: VisaActivationRepository,
        val cardId: String,
        val session: CardSession,
    )

    override fun run(session: CardSession, callback: CompletionCallback<VisaCardActivationResponse>) {
        coroutineScope.launch {
            callback(runSuspend(session))
        }
    }

    private suspend fun runSuspend(session: CardSession): CompletionResult<VisaCardActivationResponse> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        if (card.cardId != activationInput.cardId) {
            return CompletionResult.Failure(TangemSdkError.Underlying(VisaActivationError.WrongCard.message))
        }

        val visaActivationRepository = visaActivationRepositoryFactory.create(card.cardId)

        val context = SessionContext(
            visaActivationRepository = visaActivationRepository,
            cardId = card.cardId,
            session = session,
        )

        val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
            if (challengeToSign != null) {
                context.signAuthorizationChallenge(challengeToSign)
            } else {
                val activationOrder = runCatching { visaActivationRepository.getActivationOrderToSign() }
                    .getOrElse {
                        return CompletionResult.Failure(TangemSdkError.Underlying(it.message ?: ""))
                    }

                context.signOrder(activationOrder)
            }
        }
        Timber.i("VisaCardActivationTask all time: ${timedResult.duration}")
        return timedResult.value
    }

    private suspend fun SessionContext.signAuthorizationChallenge(
        challengeToSign: VisaAuthChallenge.Card,
    ): CompletionResult<VisaCardActivationResponse> {
        val attestationCommand = AttestCardKeyCommand(challenge = CryptoUtils.generateRandomBytes(length = 16))
        val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
            suspendCancellableCoroutine { continuation ->
                attestationCommand.run(session = session) { attestationResponse ->
                    continuation.resume(attestationResponse)
                }
            }
        }
        Timber.i("AttestCardKeyCommand time: ${timedResult.duration}")

        return when (val result = timedResult.value) {
            is CompletionResult.Success -> {
                Timber.i("AttestCardKeyCommand success")
                processSignedAuthorizationChallenge(
                    signedChallenge = challengeToSign.toSignedChallenge(
                        signedChallenge = result.data.cardSignature.toHexString(),
                        salt = result.data.salt.toHexString(),
                    ),
                )
            }
            is CompletionResult.Failure -> {
                Timber.e("AttestCardKeyCommand failure ${result.error}")
                CompletionResult.Failure(result.error)
            }
        }
    }

    private suspend fun SessionContext.processSignedAuthorizationChallenge(
        signedChallenge: VisaAuthSignedChallenge,
    ): CompletionResult<VisaCardActivationResponse> {
        return coroutineScope {
            val activationOrderDeferred = async { getActivationOrderToSign(signedChallenge) }
            val otpTaskDeferred = async { createWallet(session) }

            val order = runCatching { activationOrderDeferred.await() }
                .getOrElse {
                    otpTaskDeferred.cancel()
                    return@coroutineScope CompletionResult.Failure(TangemSdkError.Underlying(it.message ?: ""))
                }

            otpTaskDeferred.await()

            signOrder(order)
        }
    }

    @Throws(TangemSdkError::class)
    private suspend fun SessionContext.getActivationOrderToSign(
        signedChallenge: VisaAuthSignedChallenge,
    ): ActivationOrder {
        val tokens = runCatching {
            visaAuthRepository.getAccessTokens(signedChallenge)
        }.getOrElse {
            throw TangemSdkError.Underlying(
                "Underlying network error:  ${it.message ?: ""}",
            )
        }

        visaAuthTokenStorage.store(cardId, tokens)

        return visaActivationRepository.getActivationOrderToSign()
    }

    private suspend fun SessionContext.createWallet(session: CardSession): CompletionResult<Unit> {
        coroutineScope { ensureActive() }

        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        return if (card.wallets.any { it.curve == VisaUtilities.mandatoryCurve }) {
            createOTP(session)
        } else {
            val createWalletTask = CreateWalletTask(VisaUtilities.mandatoryCurve)

            val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
                suspendCancellableCoroutine { continuation ->
                    createWalletTask.run(session) { createWalletResult ->
                        continuation.resume(createWalletResult)
                    }
                }
            }

            Timber.i("CreateWalletTask time: ${timedResult.duration}")

            when (val result = timedResult.value) {
                is CompletionResult.Success -> {
                    Timber.i("CreateWalletTask success")
                    createOTP(session)
                }
                is CompletionResult.Failure -> {
                    Timber.e("CreateWalletTask failure ${result.error}")
                    CompletionResult.Failure(result.error)
                }
            }
        }
    }

    private suspend fun SessionContext.createOTP(session: CardSession): CompletionResult<Unit> {
        coroutineScope { ensureActive() }

        return if (otpStorage.hasSavedOTP(cardId)) {
            CompletionResult.Success(Unit)
        } else {
            val otpCommand = GenerateOTPCommand()
            val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
                suspendCancellableCoroutine { continuation ->
                    otpCommand.run(session) { otpResult ->
                        continuation.resume(otpResult)
                    }
                }
            }

            Timber.i("GenerateOTPCommand time: ${timedResult.duration}")

            when (val result = timedResult.value) {
                is CompletionResult.Success -> {
                    Timber.i("GenerateOTPCommand success")
                    otpStorage.saveOTP(cardId, result.data.rootOTP)
                    CompletionResult.Success(Unit)
                }
                is CompletionResult.Failure -> {
                    Timber.e("GenerateOTPCommand failure ${result.error}")
                    CompletionResult.Failure(result.error)
                }
            }
        }
    }

    private suspend fun SessionContext.signOrder(order: ActivationOrder): CompletionResult<VisaCardActivationResponse> {
        val card =
            session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        val wallet =
            card.wallets.firstOrNull { it.curve == VisaUtilities.mandatoryCurve }
                ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        val task = SignHashCommand(
            hash = order.hash.hexToBytes(),
            walletPublicKey = wallet.publicKey,
            derivationPath = VisaUtilities.visaDefaultDerivationPath,
        )

        val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
            suspendCancellableCoroutine { continuation ->
                task.run(session) { signResult ->
                    continuation.resume(signResult)
                }
            }
        }

        Timber.i("SignHashCommand time: ${timedResult.duration}")

        return when (val result = timedResult.value) {
            is CompletionResult.Success -> {
                Timber.i("SignHashCommand success")
                handleSignedOrder(
                    activationOrder = order,
                    response = result.data,
                )
            }
            is CompletionResult.Failure -> {
                Timber.e("SignHashCommand failure ${result.error}")
                CompletionResult.Failure(result.error)
            }
        }
    }

    private suspend fun SessionContext.handleSignedOrder(
        activationOrder: ActivationOrder,
        response: SignHashResponse,
    ): CompletionResult<VisaCardActivationResponse> {
        val signedOrder = SignedActivationOrder(
            activationOrder = activationOrder,
            signature = response.signature.toHexString(),
        )

        val otp = otpStorage.getOTP(cardId) ?: return CompletionResult.Failure(
            TangemSdkError.Underlying(VisaActivationError.MissingRootOTP.message),
        )

        val activationResponse = VisaCardActivationResponse(
            signedActivationOrder = signedOrder,
            rootOTP = VisaRootOTP(otp.toHexString()),
        )

        return setupAccessCode(session).map { activationResponse }
    }

    private suspend fun SessionContext.setupAccessCode(session: CardSession): CompletionResult<Unit> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        if (card.isAccessCodeSet) {
            return CompletionResult.Success(Unit)
        }

        Timber.i("Setting access code")

        val task = SetUserCodeCommand.changeAccessCode(accessCode)

        val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
            suspendCancellableCoroutine { continuation ->
                task.run(session) { setAccessCodeResult ->
                    continuation.resume(setAccessCodeResult)
                }
            }
        }

        Timber.i("SetUserCodeCommand time: ${timedResult.duration}")

        return when (val result = timedResult.value) {
            is CompletionResult.Success -> {
                Timber.i("SetUserCodeCommand success")
                CompletionResult.Success(Unit)
            }
            is CompletionResult.Failure -> {
                Timber.i("SetUserCodeCommand failure ${result.error}")
                CompletionResult.Failure(result.error)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            accessCode: String,
            challengeToSign: VisaAuthChallenge.Card?,
            activationInput: VisaActivationInput,
            coroutineScope: CoroutineScope,
        ): VisaCardActivationTask
    }
}