package com.tangem.tap.domain.tasks.visa

import com.reown.util.hexToBytes
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toHexString
import com.tangem.common.map
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.datasource.local.visa.VisaOTPStorage
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
import kotlin.coroutines.resume
import kotlin.jvm.Throws

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
        val card: Card,
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
            card = card,
            session = session,
        )

        return if (challengeToSign != null) {
            context.signAuthorizationChallenge(challengeToSign)
        } else {
            val activationOrder = runCatching { visaActivationRepository.getActivationOrderToSign() }
                .getOrElse {
                    return CompletionResult.Failure(TangemSdkError.Underlying(it.message ?: ""))
                }

            context.signOrder(activationOrder)
        }
    }

    private suspend fun SessionContext.signAuthorizationChallenge(
        challengeToSign: VisaAuthChallenge.Card,
    ): CompletionResult<VisaCardActivationResponse> {
        val attestationCommand = AttestCardKeyCommand(challenge = challengeToSign.challenge.hexToBytes())
        val result = suspendCancellableCoroutine { continuation ->
            attestationCommand.run(session = session) { attestationResponse ->
                continuation.resume(attestationResponse)
            }
        }

        return when (result) {
            is CompletionResult.Success -> {
                processSignedAuthorizationChallenge(
                    signedChallenge = challengeToSign.toSignedChallenge(
                        signedChallenge = result.data.cardSignature.toHexString(),
                        salt = result.data.salt.toHexString(),
                    ),
                )
            }
            is CompletionResult.Failure -> {
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

        visaAuthTokenStorage.store(card.cardId, tokens)

        return visaActivationRepository.getActivationOrderToSign()
    }

    private suspend fun createWallet(session: CardSession): CompletionResult<Unit> {
        coroutineScope { ensureActive() }

        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        return if (card.wallets.any { it.curve == VisaUtilities.mandatoryCurve }) {
            createOTP(session)
        } else {
            val createWalletTask = CreateWalletTask(VisaUtilities.mandatoryCurve)
            val result = suspendCancellableCoroutine { continuation ->
                createWalletTask.run(session) { createWalletResult ->
                    continuation.resume(createWalletResult)
                }
            }

            when (result) {
                is CompletionResult.Success -> {
                    createOTP(session)
                }
                is CompletionResult.Failure -> {
                    CompletionResult.Failure(result.error)
                }
            }
        }
    }

    private suspend fun createOTP(session: CardSession): CompletionResult<Unit> {
        coroutineScope { ensureActive() }

        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        val otp = otpStorage.getOTP(card.cardId)
        return if (otp != null) {
            CompletionResult.Success(Unit)
        } else {
            val otpCommand = GenerateOTPCommand()
            val result = suspendCancellableCoroutine { continuation ->
                otpCommand.run(session) { otpResult ->
                    continuation.resume(otpResult)
                }
            }

            when (result) {
                is CompletionResult.Success -> {
                    otpStorage.saveOTP(card.cardId, result.data.rootOTP)
                    CompletionResult.Success(Unit)
                }
                is CompletionResult.Failure -> {
                    CompletionResult.Failure(result.error)
                }
            }
        }
    }

    private suspend fun SessionContext.signOrder(order: ActivationOrder): CompletionResult<VisaCardActivationResponse> {
        val wallet =
            card.wallets.firstOrNull() ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        val task = SignHashCommand(order.hash.hexToBytes(), wallet.publicKey)
        val result = suspendCancellableCoroutine { continuation ->
            task.run(session) { signResult ->
                continuation.resume(signResult)
            }
        }

        return when (result) {
            is CompletionResult.Success -> {
                handleSignedOrder(
                    activationOrder = order,
                    response = result.data,
                )
            }
            is CompletionResult.Failure -> {
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

        val otp = otpStorage.getOTP(card.cardId) ?: return CompletionResult.Failure(
            TangemSdkError.Underlying(VisaActivationError.MissingRootOTP.message),
        )

        val activationResponse = VisaCardActivationResponse(
            signedActivationOrder = signedOrder,
            rootOTP = VisaRootOTP(otp.toHexString()),
        )

        return setupAccessCode(session).map { activationResponse }
    }

    private suspend fun SessionContext.setupAccessCode(session: CardSession): CompletionResult<Unit> {
        if (card.isAccessCodeSet) {
            return CompletionResult.Success(Unit)
        }

        val task = SetUserCodeCommand.changeAccessCode(accessCode)
        val result = suspendCancellableCoroutine { continuation ->
            task.run(session) { setAccessCodeResult ->
                continuation.resume(setAccessCodeResult)
            }
        }

        return when (result) {
            is CompletionResult.Success -> {
                CompletionResult.Success(Unit)
            }
            is CompletionResult.Failure -> {
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